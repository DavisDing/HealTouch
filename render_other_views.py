# -*- coding: utf-8 -*-
import subprocess
from generate_all_views import svg_header, make_shell

# 04. 账单与收费管理 (Bills & Cashier)
def build_bill_view():
    shell = make_shell(active_nav="bills")
    content = """
  <!-- Main Bills Management Area -->
  <g transform="translate(252, 92)">
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">账单与收银管理</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">门诊收费原则：全额收齐、严禁欠费、支持预存/现金/微信/支付宝组合结算与多联收据打印</text>

    <!-- Top KPI Cards -->
    <g transform="translate(0, 68)">
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="270" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">今日实收账单</text>
        <text x="20" y="66" class="font-sans" font-size="28" font-weight="800" fill="#13334A">¥ 4,860.00</text>
        <text x="200" y="66" class="font-sans" font-size="12" font-weight="600" fill="#087F6D">16 笔已结</text>
      </g>
      <g transform="translate(290, 0)">
        <rect x="0" y="0" width="270" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">待收单据 (30分钟超时作废)</text>
        <text x="20" y="66" class="font-sans" font-size="28" font-weight="800" fill="#D97706">1 <tspan font-size="16" font-weight="500">笔待结</tspan></text>
        <text x="180" y="66" class="font-sans" font-size="12" font-weight="600" fill="#D97706">¥ 180.00</text>
      </g>
      <g transform="translate(580, 0)">
        <rect x="0" y="0" width="270" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">预存账户划扣结算</text>
        <text x="20" y="66" class="font-sans" font-size="28" font-weight="800" fill="#087F6D">¥ 1,820.00</text>
        <text x="210" y="66" class="font-sans" font-size="12" fill="#718E9E">占比 37.4%</text>
      </g>
      <g transform="translate(870, 0)">
        <rect x="0" y="0" width="270" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">今日退款冲销</text>
        <text x="20" y="66" class="font-sans" font-size="28" font-weight="800" fill="#DC2626">¥ 0.00</text>
        <text x="210" y="66" class="font-sans" font-size="12" fill="#16A34A">无异常冲正</text>
      </g>
    </g>

    <!-- Filter & Search Toolbar -->
    <g transform="translate(0, 180)">
      <rect x="0" y="0" width="1140" height="60" rx="10" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      <g transform="translate(16, 12)">
        <rect x="0" y="0" width="240" height="36" rx="6" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
        <text x="14" y="23" class="font-sans" font-size="12" fill="#8CA4B2">搜索账单号 / 患者姓名 / 手机号</text>
      </g>
      <g transform="translate(270, 12)">
        <rect x="0" y="0" width="150" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
        <text x="14" y="23" class="font-sans" font-size="12" fill="#2C485A">单据状态: 全部状态</text>
      </g>
      <g transform="translate(435, 12)">
        <rect x="0" y="0" width="170" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
        <text x="14" y="23" class="font-sans" font-size="12" fill="#2C485A">支付方式: 全部渠道</text>
      </g>
      <g transform="translate(620, 12)">
        <rect x="0" y="0" width="80" height="36" rx="6" fill="#087F6D"/>
        <text x="40" y="23" class="font-sans" font-size="12" font-weight="600" fill="#FFFFFF" text-anchor="middle">查询</text>
      </g>
      <g transform="translate(980, 12)">
        <rect x="0" y="0" width="144" height="36" rx="6" fill="#F0F8F6" stroke="#087F6D" stroke-width="1"/>
        <text x="72" y="23" class="font-sans" font-size="12" font-weight="700" fill="#087F6D" text-anchor="middle">🖨 批量补打发票凭单</text>
      </g>
    </g>

    <!-- Bills Table Area -->
    <g transform="translate(0, 256)">
      <rect x="0" y="0" width="1140" height="520" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      
      <!-- Table Header -->
      <rect x="16" y="16" width="1108" height="40" rx="6" fill="#EDF6F4"/>
      <text x="32" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">账单编号</text>
      <text x="180" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">患者信息</text>
      <text x="310" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">关联推拿服务 / 主治医师</text>
      <text x="560" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">应收金额</text>
      <text x="650" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">实收金额</text>
      <text x="740" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">支付方式拆分</text>
      <text x="890" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">结算时间 / 收银人</text>
      <text x="1030" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">状态 / 操作</text>

      <!-- Row 1: 待支付 (超期预警) -->
      <rect x="16" y="62" width="1108" height="56" rx="8" fill="#FFFDF8" stroke="#FDE68A" stroke-width="1.2"/>
      <text x="32" y="95" class="font-sans" font-size="12" font-weight="700" fill="#D97706">B20261018-005</text>
      <text x="180" y="95" class="font-sans" font-size="13" font-weight="700" fill="#13334A">刘浩宇 <tspan font-size="11" font-weight="400" fill="#D97706">(小儿)</tspan></text>
      <text x="310" y="95" class="font-sans" font-size="12" fill="#28495C">小儿积食调理 + 穴位敷贴 · 王春兰</text>
      <text x="560" y="95" class="font-sans" font-size="13" font-weight="700" fill="#13334A">¥ 180.00</text>
      <text x="650" y="95" class="font-sans" font-size="13" font-weight="700" fill="#D97706">待结算</text>
      <text x="740" y="95" class="font-sans" font-size="12" fill="#7893A2">待确认支付方式</text>
      <text x="890" y="88" class="font-sans" font-size="12" fill="#2C485A">14:15 开单 (李医生)</text>
      <text x="890" y="104" class="font-sans" font-size="11" fill="#DC2626">已开单 8 分钟 (限30分)</text>
      <rect x="1025" y="76" width="76" height="28" rx="6" fill="#087F6D"/>
      <text x="1063" y="94" class="font-sans" font-size="11" font-weight="700" fill="#FFFFFF" text-anchor="middle">立即收银</text>

      <!-- Row 2: 已支付 -->
      <line x1="28" y1="126" x2="1112" y2="126" stroke="#EDF2F5" stroke-width="1"/>
      <text x="32" y="156" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">B20261018-004</text>
      <text x="180" y="156" class="font-sans" font-size="13" font-weight="700" fill="#13334A">张伟 <tspan font-size="11" font-weight="400" fill="#668292">(成人)</tspan></text>
      <text x="310" y="156" class="font-sans" font-size="12" fill="#28495C">颈肩综合推拿理疗 · 周明</text>
      <text x="560" y="156" class="font-sans" font-size="13" font-weight="600" fill="#13334A">¥ 260.00</text>
      <text x="650" y="156" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 260.00</text>
      <text x="740" y="156" class="font-sans" font-size="12" fill="#2E4D60">微信扫码支付 ¥260.00</text>
      <text x="890" y="150" class="font-sans" font-size="12" fill="#2C485A">13:40:22 / 张前台</text>
      <text x="890" y="165" class="font-sans" font-size="11" fill="#718E9E">凭证号 #WX98231</text>
      <rect x="1025" y="142" width="76" height="28" rx="6" fill="#F0F5F8" stroke="#D3E2EA" stroke-width="1"/>
      <text x="1063" y="160" class="font-sans" font-size="11" font-weight="600" fill="#426071" text-anchor="middle">详情 / 打印</text>

      <!-- Row 3: 已支付 (预存扣款) -->
      <line x1="28" y1="188" x2="1112" y2="188" stroke="#EDF2F5" stroke-width="1"/>
      <text x="32" y="218" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">B20261018-003</text>
      <text x="180" y="218" class="font-sans" font-size="13" font-weight="700" fill="#13334A">王丽华 <tspan font-size="11" font-weight="400" fill="#668292">(成人)</tspan></text>
      <text x="310" y="218" class="font-sans" font-size="12" fill="#28495C">腰椎间盘突出保守推拿 · 李振华</text>
      <text x="560" y="218" class="font-sans" font-size="13" font-weight="600" fill="#13334A">¥ 380.00</text>
      <text x="650" y="218" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 380.00</text>
      <text x="740" y="218" class="font-sans" font-size="12" fill="#087F6D">预存划扣 (余¥2,420.00)</text>
      <text x="890" y="212" class="font-sans" font-size="12" fill="#2C485A">11:55:10 / 李医生</text>
      <text x="890" y="227" class="font-sans" font-size="11" fill="#718E9E">流水 #DEP-20261018-02</text>
      <rect x="1025" y="204" width="76" height="28" rx="6" fill="#F0F5F8" stroke="#D3E2EA" stroke-width="1"/>
      <text x="1063" y="222" class="font-sans" font-size="11" font-weight="600" fill="#426071" text-anchor="middle">详情 / 打印</text>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

# 05. 预存账户管理 (Deposits)
def build_deposit_view():
    shell = make_shell(active_nav="deposits")
    content = """
  <!-- Main Deposit Management Area -->
  <g transform="translate(252, 92)">
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">预存账户管理</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">患者预存金充值、消费抵扣、退款审批与全生命周期双写不可篡改审计流水</text>

    <!-- Top Total Summary Cards -->
    <g transform="translate(0, 68)">
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="360" height="110" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="24" y="32" class="font-sans" font-size="13" font-weight="600" fill="#718E9E">全院患者在库总预存金</text>
        <text x="24" y="74" class="font-sans" font-size="32" font-weight="800" fill="#087F6D">¥ 284,560.00</text>
        <text x="24" y="96" class="font-sans" font-size="11" fill="#8CA4B2">持仓储值患者 192 人 · 平均余额 ¥ 1,482.08</text>
      </g>

      <g transform="translate(390, 0)">
        <rect x="0" y="0" width="360" height="110" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="24" y="32" class="font-sans" font-size="13" font-weight="600" fill="#718E9E">本月充值 / 消费划扣流水</text>
        <text x="24" y="74" class="font-sans" font-size="32" font-weight="800" fill="#13334A">+ ¥ 42,000 / - ¥ 31,400</text>
        <text x="24" y="96" class="font-sans" font-size="11" fill="#8CA4B2">储值充赠遵循财务合规入账 · 净增 ¥ 10,600.00</text>
      </g>

      <g transform="translate(780, 0)">
        <rect x="0" y="0" width="360" height="110" rx="12" fill="url(#primaryGrad)" class="shadow-md"/>
        <text x="24" y="34" class="font-sans" font-size="16" font-weight="700" fill="#FFFFFF">办理患者预存充值 / 调整</text>
        <text x="24" y="58" class="font-sans" font-size="12" fill="#E2F4F0">输入患者手机号/档案号即刻充值入账</text>
        <rect x="24" y="70" width="130" height="28" rx="6" fill="#FFFFFF"/>
        <text x="89" y="88" class="font-sans" font-size="11" font-weight="700" fill="#087F6D" text-anchor="middle">＋ 快速新建充值</text>
      </g>
    </g>

    <!-- Master-Detail Table: Left Accounts (680px), Right Ledger Audit (450px) -->
    <g transform="translate(0, 198)">
      <!-- Left: Patient Balance List -->
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="680" height="580" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <rect x="16" y="16" width="648" height="38" rx="6" fill="#EDF6F4"/>
        <text x="32" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">患者姓名 / 类型</text>
        <text x="180" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">联系电话</text>
        <text x="300" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">当前可用预存额</text>
        <text x="440" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">最近变动时间</text>
        <text x="580" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">操作</text>

        <!-- Row 1 Selected: 刘浩宇 -->
        <rect x="16" y="60" width="648" height="52" rx="8" fill="#F0F8F6" stroke="#A9DDD1" stroke-width="1.5"/>
        <text x="32" y="91" class="font-sans" font-size="13" font-weight="700" fill="#13334A">刘浩宇 <tspan font-size="11" font-weight="600" fill="#D97706">[小儿]</tspan></text>
        <text x="180" y="91" class="font-sans" font-size="12" fill="#2D4D5F">138****6621</text>
        <text x="300" y="91" class="font-sans" font-size="13" font-weight="800" fill="#087F6D">¥ 1,480.00</text>
        <text x="440" y="91" class="font-sans" font-size="11" fill="#718E9E">今日 11:00 消费扣款</text>
        <rect x="575" y="73" width="70" height="26" rx="4" fill="#087F6D"/>
        <text x="610" y="90" class="font-sans" font-size="11" font-weight="700" fill="#FFFFFF" text-anchor="middle">充值</text>

        <!-- Row 2: 王丽华 -->
        <rect x="16" y="118" width="648" height="52" rx="6" fill="#FFFFFF"/>
        <line x1="28" y1="170" x2="652" y2="170" stroke="#F1F5F8" stroke-width="1"/>
        <text x="32" y="149" class="font-sans" font-size="13" font-weight="700" fill="#13334A">王丽华 <tspan font-size="11" font-weight="400" fill="#718E9E">[成人]</tspan></text>
        <text x="180" y="149" class="font-sans" font-size="12" fill="#2D4D5F">139****1122</text>
        <text x="300" y="149" class="font-sans" font-size="13" font-weight="800" fill="#087F6D">¥ 2,420.00</text>
        <text x="440" y="149" class="font-sans" font-size="11" fill="#718E9E">今日 11:55 消费扣款</text>
        <rect x="575" y="131" width="70" height="26" rx="4" fill="#F0F5F8" stroke="#D3E2EA" stroke-width="1"/>
        <text x="610" y="148" class="font-sans" font-size="11" font-weight="600" fill="#4B6A7B" text-anchor="middle">充值</text>
      </g>

      <!-- Right: Detailed Transaction Ledger for Selected Account -->
      <g transform="translate(700, 0)">
        <rect x="0" y="0" width="440" height="580" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="32" class="font-sans" font-size="15" font-weight="700" fill="#17384D">刘浩宇 · 预存资金对账流水 (双写审计)</text>
        <text x="20" y="52" class="font-sans" font-size="11" fill="#718E9E">账户开立日期: 2026-08-20 · 累计充值: ¥ 3,000.00 · 累计消费: ¥ 1,520.00</text>

        <!-- Ledger 1 -->
        <g transform="translate(20, 72)">
          <rect x="0" y="0" width="400" height="74" rx="8" fill="#F8FAFC" stroke="#E2EBF0" stroke-width="1"/>
          <text x="14" y="24" class="font-sans" font-size="12" font-weight="700" fill="#13334A">治疗开单划扣结算</text>
          <text x="310" y="24" class="font-sans" font-size="14" font-weight="800" fill="#DC2626">- ¥ 120.00</text>
          <text x="14" y="44" class="font-sans" font-size="11" fill="#718E9E">单据: 账单 #B20261018-002 (小儿积食调理)</text>
          <text x="14" y="62" class="font-sans" font-size="10" fill="#94A8B5">2026-10-18 11:00:24 · 操作人: 王春兰 · 划扣后余额: ¥ 1,480.00</text>
        </g>

        <!-- Ledger 2 -->
        <g transform="translate(20, 156)">
          <rect x="0" y="0" width="400" height="74" rx="8" fill="#F8FAFC" stroke="#E2EBF0" stroke-width="1"/>
          <text x="14" y="24" class="font-sans" font-size="12" font-weight="700" fill="#13334A">治疗开单划扣结算</text>
          <text x="310" y="24" class="font-sans" font-size="14" font-weight="800" fill="#DC2626">- ¥ 120.00</text>
          <text x="14" y="44" class="font-sans" font-size="11" fill="#718E9E">单据: 账单 #B20261012-005 (小儿健脾调理)</text>
          <text x="14" y="62" class="font-sans" font-size="10" fill="#94A8B5">2026-10-12 15:30:11 · 操作人: 王春兰 · 划扣后余额: ¥ 1,600.00</text>
        </g>

        <!-- Ledger 3 -->
        <g transform="translate(20, 240)">
          <rect x="0" y="0" width="400" height="74" rx="8" fill="#F2FBF8" stroke="#B4E5D9" stroke-width="1"/>
          <text x="14" y="24" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">门诊前台账户充值 (微信支付)</text>
          <text x="300" y="24" class="font-sans" font-size="14" font-weight="800" fill="#087F6D">+ ¥ 2,000.00</text>
          <text x="14" y="44" class="font-sans" font-size="11" fill="#718E9E">凭证号: WX20261001-993 · 赠送小儿药浴包一份</text>
          <text x="14" y="62" class="font-sans" font-size="10" fill="#94A8B5">2026-10-01 10:14:05 · 操作人: 张前台 · 充值后余额: ¥ 2,000.00</text>
        </g>

        <!-- Bottom Audit Integrity Seal -->
        <g transform="translate(20, 500)">
          <rect x="0" y="0" width="400" height="54" rx="6" fill="#F0F8F6"/>
          <circle cx="20" cy="27" r="8" fill="#087F6D"/>
          <path d="M16 27 L19 30 L24 24" stroke="#FFFFFF" stroke-width="1.5" fill="none"/>
          <text x="36" y="24" class="font-sans" font-size="11" font-weight="700" fill="#087F6D">本地 SQLite ACID 事务保证与流水防篡改校验正常</text>
          <text x="36" y="42" class="font-sans" font-size="10" fill="#668292">当前账户流水总计与结存余额 100% 吻合 (差额 ¥0.00)</text>
        </g>
      </g>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

# 06. 退款审批与更正 (Refunds)
def build_refund_view():
    shell = make_shell(active_nav="refunds")
    content = """
  <!-- Main Refund & Correction Area -->
  <g transform="translate(252, 92)">
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">退款审批与异常更正</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">财务风控规范：退款必须由前台/治疗师发起申请，经系统管理员审批后方可原路冲正退回，全程留痕</text>

    <!-- Top Stats -->
    <g transform="translate(0, 68)">
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="360" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">待审批退款申请</text>
        <text x="20" y="66" class="font-sans" font-size="28" font-weight="800" fill="#D97706">1 <tspan font-size="16" font-weight="500">笔待办</tspan></text>
        <text x="180" y="66" class="font-sans" font-size="12" font-weight="600" fill="#D97706">涉及金额 ¥ 260.00</text>
      </g>
      <g transform="translate(390, 0)">
        <rect x="0" y="0" width="360" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">本月累计退款总额</text>
        <text x="20" y="66" class="font-sans" font-size="28" font-weight="800" fill="#13334A">¥ 520.00</text>
        <text x="220" y="66" class="font-sans" font-size="12" fill="#718E9E">已审批执行 2 笔</text>
      </g>
      <g transform="translate(780, 0)">
        <rect x="0" y="0" width="360" height="96" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="30" class="font-sans" font-size="12" font-weight="600" fill="#718E9E">退款审批权限判定</text>
        <text x="20" y="66" class="font-sans" font-size="20" font-weight="700" fill="#087F6D">当前账号具有终审权限</text>
        <text x="240" y="66" class="font-sans" font-size="12" fill="#087F6D">(管理员身份)</text>
      </g>
    </g>

    <!-- Refund Cases Table -->
    <g transform="translate(0, 184)">
      <rect x="0" y="0" width="1140" height="590" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      <rect x="16" y="16" width="1108" height="40" rx="6" fill="#EDF6F4"/>
      <text x="32" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">退款申请号</text>
      <text x="180" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">原账单号 / 患者</text>
      <text x="360" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">申请退款原因与说明</text>
      <text x="640" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">申请退款金额</text>
      <text x="760" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">原支付方式</text>
      <text x="890" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">申请人 / 时间</text>
      <text x="1020" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">审批操作</text>

      <!-- Row 1: Pending Approval -->
      <rect x="16" y="62" width="1108" height="74" rx="8" fill="#FFFDF8" stroke="#FDE68A" stroke-width="1.2"/>
      <text x="32" y="102" class="font-sans" font-size="12" font-weight="700" fill="#D97706">REF-20261018-01</text>
      <text x="180" y="96" class="font-sans" font-size="13" font-weight="700" fill="#13334A">张伟 (成人)</text>
      <text x="180" y="114" class="font-sans" font-size="11" fill="#718E9E">账单 #B20261018-004</text>
      <text x="360" y="96" class="font-sans" font-size="12" font-weight="600" fill="#243F50">患者因临时紧急突发事务改约离店，未进行颈肩推拿服务</text>
      <text x="360" y="114" class="font-sans" font-size="11" fill="#718E9E">由前台张晓华现场核实并征得主治医师周明同意</text>
      <text x="640" y="104" class="font-sans" font-size="15" font-weight="800" fill="#DC2626">¥ 260.00</text>
      <text x="760" y="102" class="font-sans" font-size="12" fill="#2E4A5C">微信支付原路冲正</text>
      <text x="890" y="96" class="font-sans" font-size="12" fill="#2C485A">张前台</text>
      <text x="890" y="114" class="font-sans" font-size="11" fill="#718E9E">14:05 (今日)</text>
      <g transform="translate(1005, 80)">
        <rect x="0" y="0" width="56" height="28" rx="4" fill="#087F6D"/>
        <text x="28" y="18" class="font-sans" font-size="11" font-weight="700" fill="#FFFFFF" text-anchor="middle">批准</text>
        <rect x="62" y="0" width="46" height="28" rx="4" fill="#FEE2E2" stroke="#FCA5A5" stroke-width="1"/>
        <text x="85" y="18" class="font-sans" font-size="11" font-weight="700" fill="#DC2626" text-anchor="middle">驳回</text>
      </g>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

# 07. 治疗项目目录与分类管理 (Catalog & Categories)
def build_catalog_view():
    shell = make_shell(active_nav="catalog")
    content = """
  <!-- Main Catalog Management Area -->
  <g transform="translate(252, 92)">
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">治疗项目与分类管理</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">配置门诊推拿理疗项目、基准单价、建议时长、适应人群（成人/小儿）与启用状态</text>

    <g transform="translate(850, 10)">
      <rect x="0" y="0" width="120" height="40" rx="8" fill="#F0F8F6" stroke="#087F6D" stroke-width="1"/>
      <text x="60" y="25" class="font-sans" font-size="12" font-weight="700" fill="#087F6D" text-anchor="middle">📁 分类管理</text>
      <rect x="135" y="0" width="150" height="40" rx="8" fill="url(#primaryGrad)"/>
      <text x="210" y="25" class="font-sans" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle">＋ 新增治疗项目</text>
    </g>

    <!-- Category Tabs Filter -->
    <g transform="translate(0, 68)">
      <rect x="0" y="0" width="1140" height="52" rx="10" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      <g transform="translate(16, 10)">
        <rect x="0" y="0" width="110" height="32" rx="6" fill="#087F6D"/>
        <text x="55" y="21" class="font-sans" font-size="12" font-weight="700" fill="#FFFFFF" text-anchor="middle">全部项目 (12)</text>
        <rect x="120" y="0" width="120" height="32" rx="6" fill="#F0F5F8"/>
        <text x="180" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">小儿推拿类 (4)</text>
        <rect x="250" y="0" width="120" height="32" rx="6" fill="#F0F5F8"/>
        <text x="310" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">成人推拿类 (5)</text>
        <rect x="380" y="0" width="120" height="32" rx="6" fill="#F0F5F8"/>
        <text x="440" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">正骨整脊类 (2)</text>
        <rect x="510" y="0" width="120" height="32" rx="6" fill="#F0F5F8"/>
        <text x="570" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">理疗辅助类 (1)</text>
      </g>
    </g>

    <!-- Catalog Table Area -->
    <g transform="translate(0, 136)">
      <rect x="0" y="0" width="1140" height="640" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      <rect x="16" y="16" width="1108" height="40" rx="6" fill="#EDF6F4"/>
      <text x="32" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">项目编码</text>
      <text x="140" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">项目名称</text>
      <text x="310" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">所属分类</text>
      <text x="440" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">适用人群</text>
      <text x="560" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">标准单价</text>
      <text x="680" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">参考时长</text>
      <text x="800" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">当前状态</text>
      <text x="960" y="41" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">操作</text>

      <!-- Row 1 -->
      <line x1="28" y1="64" x2="1112" y2="64" stroke="#EDF2F5" stroke-width="1"/>
      <text x="32" y="94" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">CAT-P01</text>
      <text x="140" y="94" class="font-sans" font-size="13" font-weight="700" fill="#13334A">小儿积食调理推拿</text>
      <text x="310" y="94" class="font-sans" font-size="12" fill="#2A5364">小儿推拿类</text>
      <rect x="440" y="79" width="46" height="20" rx="4" fill="#FEF2E2"/>
      <text x="448" y="93" class="font-sans" font-size="11" font-weight="600" fill="#C26A08">小儿</text>
      <text x="560" y="94" class="font-sans" font-size="14" font-weight="800" fill="#0C3449">¥ 120.00</text>
      <text x="680" y="94" class="font-sans" font-size="12" fill="#718E9E">30 分钟</text>
      <rect x="800" y="79" width="48" height="20" rx="4" fill="#DCFCE7"/>
      <text x="812" y="93" class="font-sans" font-size="11" font-weight="700" fill="#15803D">启用中</text>
      <g transform="translate(960, 78)">
        <text x="0" y="16" class="font-sans" font-size="12" font-weight="600" fill="#087F6D">编辑</text>
        <text x="40" y="16" class="font-sans" font-size="12" font-weight="600" fill="#DC2626">停用</text>
      </g>

      <!-- Row 2 -->
      <line x1="28" y1="116" x2="1112" y2="116" stroke="#EDF2F5" stroke-width="1"/>
      <text x="32" y="146" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">CAT-A02</text>
      <text x="140" y="146" class="font-sans" font-size="13" font-weight="700" fill="#13334A">成人脊柱颈肩推拿</text>
      <text x="310" y="146" class="font-sans" font-size="12" fill="#2A5364">成人推拿类</text>
      <rect x="440" y="131" width="46" height="20" rx="4" fill="#E8F4FD"/>
      <text x="448" y="145" class="font-sans" font-size="11" font-weight="600" fill="#1E40AF">成人</text>
      <text x="560" y="146" class="font-sans" font-size="14" font-weight="800" fill="#0C3449">¥ 260.00</text>
      <text x="680" y="146" class="font-sans" font-size="12" fill="#718E9E">45 分钟</text>
      <rect x="800" y="131" width="48" height="20" rx="4" fill="#DCFCE7"/>
      <text x="812" y="145" class="font-sans" font-size="11" font-weight="700" fill="#15803D">启用中</text>
      <g transform="translate(960, 130)">
        <text x="0" y="16" class="font-sans" font-size="12" font-weight="600" fill="#087F6D">编辑</text>
        <text x="40" y="16" class="font-sans" font-size="12" font-weight="600" fill="#DC2626">停用</text>
      </g>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

# 08. 报表与统计 (Reports)
def build_report_view():
    shell = make_shell(active_nav="reports")
    content = """
  <!-- Main Report & Analytics Area -->
  <g transform="translate(252, 92)">
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">门诊经营与财务统计</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">多维度经营核算：营业额走势、治疗师接诊绩效工分、预存现金流及推拿项目排行榜</text>

    <!-- Top Time Selector Bar -->
    <g transform="translate(0, 68)">
      <rect x="0" y="0" width="1140" height="52" rx="10" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      <g transform="translate(16, 10)">
        <rect x="0" y="0" width="90" height="32" rx="6" fill="#087F6D"/>
        <text x="45" y="21" class="font-sans" font-size="12" font-weight="700" fill="#FFFFFF" text-anchor="middle">今日汇总</text>
        <rect x="100" y="0" width="90" height="32" rx="6" fill="#F0F5F8"/>
        <text x="145" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">本周统计</text>
        <rect x="200" y="0" width="90" height="32" rx="6" fill="#F0F5F8"/>
        <text x="245" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">本月汇总</text>
        <rect x="300" y="0" width="90" height="32" rx="6" fill="#F0F5F8"/>
        <text x="345" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3D5B6D" text-anchor="middle">自定义区间</text>
      </g>
      <g transform="translate(980, 10)">
        <rect x="0" y="0" width="144" height="32" rx="6" fill="#F8FAFC" stroke="#D3E2EA" stroke-width="1"/>
        <text x="72" y="21" class="font-sans" font-size="12" font-weight="600" fill="#3E5C6E" text-anchor="middle">⬇ 导出财务分析报表</text>
      </g>
    </g>

    <!-- Visual Charts Grid (Top Row: 2 Big Cards) -->
    <g transform="translate(0, 136)">
      <!-- Left Chart: 7-Day Revenue Trend -->
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="670" height="320" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="32" class="font-sans" font-size="15" font-weight="700" fill="#17384D">门诊近 7 日净收入走势 (已扣除退款)</text>
        <text x="560" y="32" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">周总额 ¥ 32,840</text>
        
        <!-- SVG Polyline Chart -->
        <g transform="translate(30, 70)">
          <!-- Axis lines -->
          <line x1="40" y1="20" x2="600" y2="20" stroke="#F1F5F8" stroke-width="1"/>
          <line x1="40" y1="80" x2="600" y2="80" stroke="#F1F5F8" stroke-width="1"/>
          <line x1="40" y1="140" x2="600" y2="140" stroke="#F1F5F8" stroke-width="1"/>
          <line x1="40" y1="200" x2="600" y2="200" stroke="#D9E5EC" stroke-width="1"/>
          
          <text x="30" y="24" class="font-sans" font-size="10" fill="#8CA4B2" text-anchor="end">6k</text>
          <text x="30" y="84" class="font-sans" font-size="10" fill="#8CA4B2" text-anchor="end">4k</text>
          <text x="30" y="144" class="font-sans" font-size="10" fill="#8CA4B2" text-anchor="end">2k</text>
          <text x="30" y="204" class="font-sans" font-size="10" fill="#8CA4B2" text-anchor="end">0</text>

          <!-- Day Labels -->
          <text x="80" y="222" class="font-sans" font-size="11" fill="#718E9E" text-anchor="middle">10-12 周一</text>
          <text x="160" y="222" class="font-sans" font-size="11" fill="#718E9E" text-anchor="middle">10-13 周二</text>
          <text x="240" y="222" class="font-sans" font-size="11" fill="#718E9E" text-anchor="middle">10-14 周三</text>
          <text x="320" y="222" class="font-sans" font-size="11" fill="#718E9E" text-anchor="middle">10-15 周四</text>
          <text x="400" y="222" class="font-sans" font-size="11" fill="#718E9E" text-anchor="middle">10-16 周五</text>
          <text x="480" y="222" class="font-sans" font-size="11" fill="#718E9E" text-anchor="middle">10-17 周六</text>
          <text x="560" y="222" class="font-sans" font-size="11" font-weight="700" fill="#087F6D" text-anchor="middle">10-18 今日</text>

          <!-- Trend Line -->
          <polyline points="80,120 160,110 240,90 320,105 400,60 480,40 560,54" fill="none" stroke="#087F6D" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="560" cy="54" r="5" fill="#087F6D" stroke="#FFFFFF" stroke-width="2"/>
          <text x="560" y="40" class="font-sans" font-size="11" font-weight="700" fill="#087F6D" text-anchor="middle">¥4,860</text>
        </g>
      </g>

      <!-- Right Chart: Therapist Performance -->
      <g transform="translate(690, 0)">
        <rect x="0" y="0" width="450" height="320" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        <text x="20" y="32" class="font-sans" font-size="15" font-weight="700" fill="#17384D">在职治疗师接诊绩效排行榜</text>

        <!-- Rank 1 -->
        <g transform="translate(20, 60)">
          <text x="0" y="16" class="font-sans" font-size="13" font-weight="700" fill="#13334A">1. 李振华 (主任推拿师)</text>
          <text x="350" y="16" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 2,080</text>
          <rect x="0" y="24" width="410" height="10" rx="5" fill="#EEF3F6"/>
          <rect x="0" y="24" width="310" height="10" rx="5" fill="#087F6D"/>
          <text x="0" y="48" class="font-sans" font-size="11" fill="#718E9E">今日接诊 8 人次 · 成人重症推拿专精</text>
        </g>

        <!-- Rank 2 -->
        <g transform="translate(20, 130)">
          <text x="0" y="16" class="font-sans" font-size="13" font-weight="700" fill="#13334A">2. 王春兰 (资深儿推师)</text>
          <text x="350" y="16" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 1,740</text>
          <rect x="0" y="24" width="410" height="10" rx="5" fill="#EEF3F6"/>
          <rect x="0" y="24" width="260" height="10" rx="5" fill="#149A84"/>
          <text x="0" y="48" class="font-sans" font-size="11" fill="#718E9E">今日接诊 7 人次 · 小儿推拿/呼吸调理</text>
        </g>

        <!-- Rank 3 -->
        <g transform="translate(20, 200)">
          <text x="0" y="16" class="font-sans" font-size="13" font-weight="700" fill="#13334A">3. 周明 (正骨推拿师)</text>
          <text x="350" y="16" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 1,040</text>
          <rect x="0" y="24" width="410" height="10" rx="5" fill="#EEF3F6"/>
          <rect x="0" y="24" width="160" height="10" rx="5" fill="#60A5FA"/>
          <text x="0" y="48" class="font-sans" font-size="11" fill="#718E9E">今日接诊 4 人次 · 骨盆调理与肌肉松解</text>
        </g>
      </g>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

with open("ui_render/04_bills.svg", "w", encoding="utf-8") as f:
    f.write(build_bill_view())

with open("ui_render/05_deposits.svg", "w", encoding="utf-8") as f:
    f.write(build_deposit_view())

with open("ui_render/06_refunds.svg", "w", encoding="utf-8") as f:
    f.write(build_refund_view())

with open("ui_render/07_catalog.svg", "w", encoding="utf-8") as f:
    f.write(build_catalog_view())

with open("ui_render/08_reports.svg", "w", encoding="utf-8") as f:
    f.write(build_report_view())

print("All views SVG written")
