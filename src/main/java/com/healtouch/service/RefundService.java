package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.*;
import com.healtouch.util.Checks;
import com.healtouch.util.Codes;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 账单退款采用申请、审批、执行三段流程；退款金额按原支付方式可退额度分配。 */
public class RefundService {
    private final DataSource dataSource; private final AuditLogDao audit = new AuditLogDao();
    public RefundService(DataSource dataSource) { this.dataSource=dataSource; }
    public long request(UserSession actor,long billId,long amountCents,String reason,String remark) { Authorization.require(actor,Permission.BILL_REFUND);Checks.required(reason,"退款原因");if(amountCents<=0)throw new IllegalArgumentException("退款金额必须大于 0");return Jdbc.inTransaction(dataSource,c->{Bill bill=loadBill(c,billId);if(bill.status!=BillStatus.PAID&&bill.status!=BillStatus.PARTIALLY_REFUNDED)throw new IllegalStateException("仅已支付或部分退款账单可申请退款");long remaining=bill.paid-bill.refunded;if(amountCents>remaining)throw new IllegalArgumentException("退款金额超过未退款实收额");Map<PaymentMethod,Long> allocated=allocate(c,billId,amountCents);long id;try(PreparedStatement ps=c.prepareStatement("INSERT INTO refund_request(refund_code,bill_id,requested_cents,reason,status,applicant_id,remark) VALUES(?,?,?,?,'PENDING_APPROVAL',?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,Codes.next("R"));ps.setLong(2,billId);ps.setLong(3,amountCents);ps.setString(4,reason);ps.setLong(5,actor.userId);ps.setString(6,remark);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("未生成退款单");id=rs.getLong(1);}}try(PreparedStatement ps=c.prepareStatement("UPDATE refund_request SET refund_code=? WHERE id=?")){ps.setString(1,Codes.sequential("R",id));ps.setLong(2,id);ps.executeUpdate();}insertRefundPayments(c,id,allocated);audit.record(c,actor.userId,"BILL_REFUND_REQUESTED","REFUND",String.valueOf(id),null,"bill="+billId+", amount="+amountCents);return id;});}

    public List<RefundSummary> list() {
        List<RefundSummary> result=new ArrayList<RefundSummary>();
        String sql="SELECT r.id,r.refund_code,r.requested_cents,r.status,b.bill_code,p.name patient_name,a.name applicant_name,ap.name approver_name FROM refund_request r JOIN bill b ON b.id=r.bill_id JOIN patient p ON p.id=b.patient_id JOIN app_user a ON a.id=r.applicant_id LEFT JOIN app_user ap ON ap.id=r.approver_id ORDER BY r.created_at DESC LIMIT 200";
        try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){
            while(rs.next()){RefundSummary x=new RefundSummary();x.id=rs.getLong("id");x.refundCode=rs.getString("refund_code");x.billCode=rs.getString("bill_code");x.patientName=rs.getString("patient_name");x.amountCents=rs.getLong("requested_cents");x.status=rs.getString("status");x.applicantName=rs.getString("applicant_name");x.approverName=rs.getString("approver_name");result.add(x);}return result;
        }catch(SQLException e){throw new IllegalStateException("查询退款单失败",e);}
    }

    public void approve(UserSession actor,long requestId,boolean approved,String remark){Authorization.require(actor,Permission.BILL_REFUND);Jdbc.inTransaction(dataSource,c->{Refund r=loadRequest(c,requestId);if(r.status==Status.EXECUTED||r.status==Status.REJECTED)throw new IllegalStateException("退款单已处理，不能重复审批");if(r.applicantId==actor.userId)throw new SecurityException("申请人不得审批自己的退款单");try(PreparedStatement ps=c.prepareStatement("UPDATE refund_request SET status=?,approver_id=?,approved_at=CURRENT_TIMESTAMP,remark=? WHERE id=? AND status='PENDING_APPROVAL'")){ps.setString(1,approved?"APPROVED":"REJECTED");ps.setLong(2,actor.userId);ps.setString(3,remark);ps.setLong(4,requestId);if(ps.executeUpdate()!=1)throw new IllegalStateException("退款单状态已变更");}audit.record(c,actor.userId,approved?"BILL_REFUND_APPROVED":"BILL_REFUND_REJECTED","REFUND",String.valueOf(requestId),null,remark);return null;});}
    public void execute(UserSession actor,long requestId){Authorization.require(actor,Permission.BILL_REFUND);Jdbc.inTransaction(dataSource,c->{Refund r=loadRequest(c,requestId);if(r.status!=Status.APPROVED)throw new IllegalStateException("仅已审批退款单可执行");if(!actor.isAdmin()&&r.approverId!=null&&r.approverId==actor.userId)throw new SecurityException("财务/主管审批的退款必须由另一名具备执行权限的人员执行");Bill b=loadBill(c,r.billId);if(b.paid-b.refunded<r.amount)throw new IllegalStateException("账单剩余可退金额不足");Map<PaymentMethod,Long> current=allocate(c,r.billId,r.amount);Map<PaymentMethod,Long> planned=refundPayments(c,r.id);if(!current.equals(planned))throw new IllegalStateException("原支付方式可退款额度已变化，请重新申请退款");for(Map.Entry<PaymentMethod,Long> e:planned.entrySet())if(e.getKey()==PaymentMethod.DEPOSIT)restoreDeposit(c,b.patientId,e.getValue(),b.id,actor.userId);try(PreparedStatement ps=c.prepareStatement("UPDATE bill SET refunded_cents=refunded_cents+?,status=CASE WHEN refunded_cents+?=paid_cents THEN 'REFUNDED' ELSE 'PARTIALLY_REFUNDED' END WHERE id=?")){ps.setLong(1,r.amount);ps.setLong(2,r.amount);ps.setLong(3,b.id);ps.executeUpdate();}try(PreparedStatement ps=c.prepareStatement("UPDATE refund_request SET status='EXECUTED',executor_id=?,executed_at=CURRENT_TIMESTAMP WHERE id=?")){ps.setLong(1,actor.userId);ps.setLong(2,r.id);ps.executeUpdate();}audit.record(c,actor.userId,"BILL_REFUND_EXECUTED","REFUND",String.valueOf(r.id),null,"bill="+b.id+", amount="+r.amount);return null;});}
    private Map<PaymentMethod,Long> allocate(Connection c,long billId,long amount)throws SQLException{
        Map<PaymentMethod,Long> eligible=originalLessExecuted(c,billId);
        Map<PaymentMethod,Long> allocation=new EnumMap<PaymentMethod,Long>(PaymentMethod.class);
        long remaining=amount;
        long deposit=eligible.containsKey(PaymentMethod.DEPOSIT)?eligible.get(PaymentMethod.DEPOSIT):0L;
        if(deposit>0){long take=Math.min(remaining,deposit);allocation.put(PaymentMethod.DEPOSIT,take);remaining-=take;}
        if(remaining==0)return allocation;
        long nonDepositTotal=0;
        for(PaymentMethod method:PaymentMethod.values())if(method!=PaymentMethod.DEPOSIT)nonDepositTotal+=Math.max(0,eligible.containsKey(method)?eligible.get(method):0L);
        if(remaining>nonDepositTotal)throw new IllegalStateException("原支付明细可退金额不足");
        long allocated=0;
        for(PaymentMethod method:PaymentMethod.values()){
            if(method==PaymentMethod.DEPOSIT)continue;
            long possible=Math.max(0,eligible.containsKey(method)?eligible.get(method):0L);
            if(possible==0)continue;
            long take=(remaining*possible)/nonDepositTotal;
            take=Math.min(take,possible); allocation.put(method,take); allocated+=take;
        }
        long remainder=remaining-allocated;
        for(PaymentMethod method:PaymentMethod.values()){
            if(remainder==0||method==PaymentMethod.DEPOSIT)continue;
            long possible=Math.max(0,eligible.containsKey(method)?eligible.get(method):0L);
            long current=allocation.containsKey(method)?allocation.get(method):0L;
            long spare=possible-current; if(spare>0){long take=Math.min(remainder,spare);allocation.put(method,current+take);remainder-=take;}
        }
        if(remainder!=0)throw new IllegalStateException("原支付明细可退金额不足");
        return allocation;
    }
    private Map<PaymentMethod,Long> originalLessExecuted(Connection c,long billId)throws SQLException{Map<PaymentMethod,Long> map=new EnumMap<PaymentMethod,Long>(PaymentMethod.class);try(PreparedStatement ps=c.prepareStatement("SELECT method,SUM(amount_cents) amount FROM payment WHERE bill_id=? GROUP BY method")){ps.setLong(1,billId);try(ResultSet rs=ps.executeQuery()){while(rs.next())map.put(PaymentMethod.valueOf(rs.getString(1)),rs.getLong(2));}}String sql="SELECT rp.payment_method,SUM(rp.amount_cents) amount FROM refund_payment rp JOIN refund_request r ON r.id=rp.refund_request_id WHERE r.bill_id=? AND r.status='EXECUTED' GROUP BY rp.payment_method";try(PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,billId);try(ResultSet rs=ps.executeQuery()){while(rs.next()){PaymentMethod m=PaymentMethod.valueOf(rs.getString(1));map.put(m,map.get(m)-rs.getLong(2));}}}return map;}
    private void insertRefundPayments(Connection c,long id,Map<PaymentMethod,Long> values)throws SQLException{try(PreparedStatement ps=c.prepareStatement("INSERT INTO refund_payment(refund_request_id,payment_method,amount_cents) VALUES(?,?,?)")){for(Map.Entry<PaymentMethod,Long> e:values.entrySet()){ps.setLong(1,id);ps.setString(2,e.getKey().name());ps.setLong(3,e.getValue());ps.addBatch();}ps.executeBatch();}}
    private Map<PaymentMethod,Long> refundPayments(Connection c,long id)throws SQLException{Map<PaymentMethod,Long> r=new EnumMap<PaymentMethod,Long>(PaymentMethod.class);try(PreparedStatement ps=c.prepareStatement("SELECT payment_method,amount_cents FROM refund_payment WHERE refund_request_id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){while(rs.next())r.put(PaymentMethod.valueOf(rs.getString(1)),rs.getLong(2));}}return r;}
    private void restoreDeposit(Connection c,long patientId,long amount,long billId,long userId)throws SQLException{try(PreparedStatement ps=c.prepareStatement("UPDATE deposit_account SET balance_cents=balance_cents+?,updated_at=CURRENT_TIMESTAMP WHERE patient_id=?")){ps.setLong(1,amount);ps.setLong(2,patientId);if(ps.executeUpdate()!=1)throw new IllegalStateException("预存账户不存在");}long after;try(PreparedStatement ps=c.prepareStatement("SELECT balance_cents FROM deposit_account WHERE patient_id=?")){ps.setLong(1,patientId);try(ResultSet rs=ps.executeQuery()){rs.next();after=rs.getLong(1);}}try(PreparedStatement ps=c.prepareStatement("INSERT INTO deposit_transaction(transaction_code,patient_id,transaction_type,amount_cents,balance_after_cents,payment_method,bill_id,remark,operator_id) VALUES(?,?,'REFUND',?,?,?,?,?,?)")){ps.setString(1,Codes.next("DT"));ps.setLong(2,patientId);ps.setLong(3,amount);ps.setLong(4,after);ps.setString(5,PaymentMethod.DEPOSIT.name());ps.setLong(6,billId);ps.setString(7,"账单退款退回预存");ps.setLong(8,userId);ps.executeUpdate();}}
    private Bill loadBill(Connection c,long id)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT id,patient_id,status,paid_cents,refunded_cents FROM bill WHERE id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("账单不存在");Bill b=new Bill();b.id=id;b.patientId=rs.getLong(2);b.status=BillStatus.valueOf(rs.getString(3));b.paid=rs.getLong(4);b.refunded=rs.getLong(5);return b;}}}
    private Refund loadRequest(Connection c,long id)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT * FROM refund_request WHERE id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("退款单不存在");Refund r=new Refund();r.id=id;r.billId=rs.getLong("bill_id");r.amount=rs.getLong("requested_cents");r.status=Status.valueOf(rs.getString("status"));r.applicantId=rs.getLong("applicant_id");long ap=rs.getLong("approver_id");r.approverId=rs.wasNull()?null:ap;return r;}}}
    private static class Bill {long id,patientId,paid,refunded;BillStatus status;} private static class Refund{long id,billId,amount,applicantId;Long approverId;Status status;} private enum Status{PENDING_APPROVAL,APPROVED,REJECTED,EXECUTED,REVOKED}
}
