# -*- coding: utf-8 -*-
import subprocess
from generate_all_views import svg_header, make_shell

def build_treatment_view():
    shell = make_shell(active_nav="treatment")
    content = """
  <!-- Main Treatment Registration & Checkout Area -->
  <g transform="translate(252, 92)">
    <!-- Header Title & Core Principle Note -->
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">治疗登记与开单</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">标准业务流程：选择患者 → 组合多项推拿理疗项目 → 选定主治治疗师 → 保存即生成待收费账单并立即结算（严禁欠费挂账）</text>

    <!-- Top Left & Right Split: Left is Registration Form (700px), Right is Live Bill Summary (430px) -->
    <g transform="translate(0, 68)">
      <!-- Left Panel: Treatment Intake Form -->
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="700" height="700" rx="14" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        
        <!-- Section 1: Patient Selection & Snapshot -->
        <g transform="translate(24, 20)">
          <text x="0" y="16" class="font-sans" font-size="15" font-weight="700" fill="#17384D">第一步：选择就诊患者</text>
          
          <g transform="translate(0, 28)">
            <rect x="0" y="0" width="480" height="38" rx="6" fill="#F4F8FA" stroke="#087F6D" stroke-width="1.5"/>
            <text x="14" y="24" class="font-sans" font-size="13" font-weight="700" fill="#13334A">刘浩宇 (小儿 · 5岁) · #P000109 · 监护人: 刘建民 (138****6621)</text>
            <circle cx="455" cy="19" r="8" fill="#E2F4F0"/>
            <path d="M451 19 L454 22 L459 16" stroke="#087F6D" stroke-width="1.5" fill="none"/>
          </g>

          <rect x="495" y="28" width="155" height="38" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
          <text x="572" y="52" class="font-sans" font-size="12" font-weight="600" fill="#284758" text-anchor="middle">🔍 切换/新建患者</text>

          <!-- Selected Patient Quick Status Ribbon -->
          <g transform="translate(0, 76)">
            <rect x="0" y="0" width="650" height="34" rx="6" fill="#F0F8F6"/>
            <text x="12" y="22" class="font-sans" font-size="11" fill="#4B6A78">过敏史提示: <tspan font-weight="700" fill="#DC2626">头孢类药物过敏</tspan> · 当前预存金余额: <tspan font-weight="700" fill="#087F6D">¥ 1,480.00 (充裕)</tspan></text>
          </g>
        </g>

        <!-- Line Separator -->
        <line x1="24" y1="146" x2="676" y2="146" stroke="#EEF3F6" stroke-width="1"/>

        <!-- Section 2: Physician & Date Setup -->
        <g transform="translate(24, 160)">
          <text x="0" y="16" class="font-sans" font-size="15" font-weight="700" fill="#17384D">第二步：就诊时间与在职治疗师</text>
          
          <g transform="translate(0, 28)">
            <!-- Date field -->
            <text x="0" y="14" class="font-sans" font-size="12" fill="#668292">就诊/治疗日期</text>
            <rect x="0" y="22" width="210" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="14" y="45" class="font-sans" font-size="12" font-weight="600" fill="#16384C">2026-10-18 (今日)</text>
            <path d="M190 38 L195 43 L200 38" stroke="#8CA4B2" stroke-width="1.5" fill="none"/>

            <!-- Therapist Field (from Active Therapists) -->
            <text x="235" y="14" class="font-sans" font-size="12" fill="#668292">责任主治治疗师 (限在职)</text>
            <rect x="235" y="22" width="240" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="249" y="45" class="font-sans" font-size="12" font-weight="700" fill="#16384C">王春兰 (资深儿推师 · 在职)</text>
            <path d="M455 38 L460 43 L465 38" stroke="#8CA4B2" stroke-width="1.5" fill="none"/>

            <!-- Session Time -->
            <text x="495" y="14" class="font-sans" font-size="12" fill="#668292">建议单次时长</text>
            <rect x="495" y="22" width="155" height="36" rx="6" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
            <text x="509" y="45" class="font-sans" font-size="12" fill="#2E4B5C">合计约 50 分钟</text>
          </g>
        </g>

        <!-- Line Separator -->
        <line x1="24" y1="246" x2="676" y2="246" stroke="#EEF3F6" stroke-width="1"/>

        <!-- Section 3: Multi-Line Treatment Projects Table -->
        <g transform="translate(24, 260)">
          <text x="0" y="16" class="font-sans" font-size="15" font-weight="700" fill="#17384D">第三步：开具治疗项目明细（支持单据多项目）</text>

          <!-- Add Item Bar -->
          <g transform="translate(0, 26)">
            <rect x="0" y="0" width="350" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="14" y="23" class="font-sans" font-size="12" fill="#8CA4B2">选择或搜索理疗项目（如小儿推拿、艾灸...）</text>
            
            <rect x="365" y="0" width="100" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="380" y="23" class="font-sans" font-size="12" fill="#224254">数量: 1 次</text>

            <rect x="480" y="0" width="170" height="36" rx="6" fill="#F0F7F6" stroke="#A6DAD0" stroke-width="1.5"/>
            <text x="565" y="23" class="font-sans" font-size="12" font-weight="700" fill="#087F6D" text-anchor="middle">＋ 加入治疗项目</text>
          </g>

          <!-- Table Header -->
          <g transform="translate(0, 74)">
            <rect x="0" y="0" width="650" height="34" rx="6" fill="#EDF6F4"/>
            <text x="16" y="22" class="font-sans" font-size="11" font-weight="700" fill="#2A5364">治疗项目名称</text>
            <text x="260" y="22" class="font-sans" font-size="11" font-weight="700" fill="#2A5364">标准单价(系统锁定)</text>
            <text x="420" y="22" class="font-sans" font-size="11" font-weight="700" fill="#2A5364">数量 / 次数</text>
            <text x="520" y="22" class="font-sans" font-size="11" font-weight="700" fill="#2A5364">项目小计</text>
            <text x="600" y="22" class="font-sans" font-size="11" font-weight="700" fill="#2A5364">操作</text>

            <!-- Line 1 -->
            <g transform="translate(0, 36)">
              <rect x="0" y="0" width="650" height="46" fill="#FFFFFF"/>
              <line x1="0" y1="46" x2="650" y2="46" stroke="#F0F4F7" stroke-width="1"/>
              <text x="16" y="28" class="font-sans" font-size="13" font-weight="700" fill="#13334A">小儿积食调理推拿</text>
              <text x="140" y="28" class="font-sans" font-size="11" fill="#7893A2">(30分钟 · 摩腹运内八卦)</text>
              <text x="260" y="28" class="font-sans" font-size="13" fill="#28485A">¥ 120.00 /次</text>
              
              <!-- Quantity Stepper -->
              <g transform="translate(415, 8)">
                <rect x="0" y="0" width="70" height="30" rx="4" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
                <text x="12" y="20" class="font-sans" font-size="14" fill="#668292">-</text>
                <text x="35" y="20" class="font-sans" font-size="13" font-weight="700" fill="#13334A" text-anchor="middle">1</text>
                <text x="56" y="20" class="font-sans" font-size="14" fill="#668292">+</text>
              </g>

              <text x="520" y="28" class="font-sans" font-size="14" font-weight="700" fill="#13334A">¥ 120.00</text>
              <text x="608" y="28" class="font-sans" font-size="11" fill="#DC2626">移除</text>
            </g>

            <!-- Line 2 -->
            <g transform="translate(0, 84)">
              <rect x="0" y="0" width="650" height="46" fill="#FFFFFF"/>
              <line x1="0" y1="46" x2="650" y2="46" stroke="#F0F4F7" stroke-width="1"/>
              <text x="16" y="28" class="font-sans" font-size="13" font-weight="700" fill="#13334A">中药本草穴位贴敷 (神阙穴)</text>
              <text x="260" y="28" class="font-sans" font-size="13" fill="#28485A">¥ 60.00 /贴</text>
              
              <!-- Quantity Stepper -->
              <g transform="translate(415, 8)">
                <rect x="0" y="0" width="70" height="30" rx="4" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
                <text x="12" y="20" class="font-sans" font-size="14" fill="#668292">-</text>
                <text x="35" y="20" class="font-sans" font-size="13" font-weight="700" fill="#13334A" text-anchor="middle">1</text>
                <text x="56" y="20" class="font-sans" font-size="14" fill="#668292">+</text>
              </g>

              <text x="520" y="28" class="font-sans" font-size="14" font-weight="700" fill="#13334A">¥ 60.00</text>
              <text x="608" y="28" class="font-sans" font-size="11" fill="#DC2626">移除</text>
            </g>
          </g>
        </g>

        <!-- Section 4: Clinical Notes -->
        <g transform="translate(24, 460)">
          <text x="0" y="16" class="font-sans" font-size="13" font-weight="700" fill="#17384D">治疗过程与随诊医嘱备注（可选）</text>
          <rect x="0" y="24" width="650" height="70" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
          <text x="14" y="46" class="font-sans" font-size="12" fill="#1F3F51">今日患儿脘腹胀满微硬，舌苔厚腻。推拿手法以清大肠、揉板门、运内八卦、摩腹为主。配合神阙贴敷调理脾胃积滞。</text>
          
          <g transform="translate(0, 106)">
            <circle cx="6" cy="10" r="3" fill="#D97706"/>
            <text x="16" y="14" class="font-sans" font-size="11" fill="#7E96A4">价格管控说明：治疗项目单价由系统项目字典锁定，严禁随意调整。折扣调整须由管理员授权确认。</text>
          </g>
        </g>

        <!-- Action Footer -->
        <g transform="translate(24, 615)">
          <rect x="0" y="0" width="200" height="44" rx="8" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
          <text x="100" y="27" class="font-sans" font-size="13" font-weight="600" fill="#587687" text-anchor="middle">重置 / 放弃开单</text>

          <rect x="220" y="0" width="430" height="44" rx="8" fill="url(#primaryGrad)" class="shadow-sm"/>
          <text x="435" y="27" class="font-sans" font-size="14" font-weight="700" fill="#FFFFFF" text-anchor="middle">✓ 保存并立即进入收费 (¥ 180.00) →</text>
        </g>
      </g>

      <!-- Right Panel: Realtime Billing & Split Payment Modal (430px) -->
      <g transform="translate(720, 0)">
        <rect x="0" y="0" width="430" height="700" rx="14" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        
        <!-- Header -->
        <g transform="translate(20, 20)">
          <rect x="0" y="0" width="70" height="22" rx="4" fill="#FEE7E6"/>
          <text x="35" y="15" class="font-sans" font-size="11" font-weight="700" fill="#DC2626" text-anchor="middle">待收费</text>
          <text x="80" y="16" class="font-sans" font-size="16" font-weight="700" fill="#13334A">门诊结算收银台</text>
          <text x="0" y="40" class="font-sans" font-size="11" fill="#8DA6B5">账单号: #B20261018-009 · 临时结算会话 (30分钟超时作废)</text>
        </g>

        <!-- Total Breakdown Card -->
        <g transform="translate(20, 72)">
          <rect x="0" y="0" width="390" height="90" rx="10" fill="url(#primaryCardGrad)" stroke="#BCE3DA" stroke-width="1"/>
          <text x="20" y="28" class="font-sans" font-size="12" fill="#5E7F8E">应收总额 (项目合计 2项)</text>
          <text x="20" y="66" class="font-sans" font-size="32" font-weight="800" fill="#0C3449">¥ 180.00</text>
          <text x="240" y="44" class="font-sans" font-size="12" fill="#6B8897">整单折扣额: <tspan font-weight="700" fill="#13334A">¥ 0.00</tspan></text>
          <text x="240" y="66" class="font-sans" font-size="12" fill="#6B8897">实收总计: <tspan font-weight="700" fill="#087F6D">¥ 180.00</tspan></text>
        </g>

        <!-- Split Payment Configuration -->
        <g transform="translate(20, 180)">
          <text x="0" y="16" class="font-sans" font-size="14" font-weight="700" fill="#17384D">支付方式拆分 (实收总额必须完全等于应收)</text>
          <text x="0" y="34" class="font-sans" font-size="11" fill="#849DAA">严禁任何形式挂账或欠费 · 所有支付行之和校验一致方可确认</text>
          
          <!-- Payment Method Line 1: 预存账户扣款 -->
          <g transform="translate(0, 48)">
            <rect x="0" y="0" width="390" height="74" rx="8" fill="#F7FAFC" stroke="#087F6D" stroke-width="1.5"/>
            <text x="14" y="24" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">方式 1: 患者预存金划扣</text>
            <text x="160" y="24" class="font-sans" font-size="11" fill="#718F9E">(当前可用余额: ¥ 1,480.00)</text>
            
            <rect x="14" y="34" width="180" height="30" rx="4" fill="#FFFFFF" stroke="#087F6D" stroke-width="1"/>
            <text x="24" y="54" class="font-sans" font-size="14" font-weight="700" fill="#13334A">¥ 180.00</text>
            <rect x="205" y="34" width="170" height="30" rx="4" fill="#D1EFE8"/>
            <text x="290" y="53" class="font-sans" font-size="11" font-weight="700" fill="#087F6D" text-anchor="middle">✓ 余额充足校验通过</text>
          </g>

          <!-- Payment Method Line 2 (Optional Split Example) -->
          <g transform="translate(0, 132)">
            <rect x="0" y="0" width="390" height="50" rx="8" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="14" y="30" class="font-sans" font-size="12" fill="#668292">方式 2: 微信 / 支付宝扫码</text>
            <rect x="180" y="10" width="110" height="30" rx="4" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
            <text x="190" y="30" class="font-sans" font-size="12" fill="#88A1B0">¥ 0.00</text>
            <text x="310" y="30" class="font-sans" font-size="11" fill="#087F6D">＋ 添加支付行</text>
          </g>

          <!-- Realtime Validation Bar -->
          <g transform="translate(0, 198)">
            <rect x="0" y="0" width="390" height="42" rx="6" fill="#DCFCE7"/>
            <circle cx="20" cy="21" r="7" fill="#16A34A"/>
            <path d="M17 21 L19 23 L23 19" stroke="#FFFFFF" stroke-width="1.5" fill="none"/>
            <text x="34" y="26" class="font-sans" font-size="12" font-weight="700" fill="#15803D">已分配支付: ¥ 180.00 = 待收 ¥ 180.00 (无差额)</text>
          </g>
        </g>

        <!-- Compliance & Traceability Audit Note -->
        <g transform="translate(20, 450)">
          <rect x="0" y="0" width="390" height="110" rx="8" fill="#FFFBF5" stroke="#F6E7D2" stroke-width="1"/>
          <text x="14" y="24" class="font-sans" font-size="12" font-weight="700" fill="#B45309">收费后自动化动作说明:</text>
          <text x="14" y="46" class="font-sans" font-size="11" fill="#7A6856">1. 账单状态即刻锁定为“已支付”，记录结算人李医生。</text>
          <text x="14" y="66" class="font-sans" font-size="11" fill="#7A6856">2. 预存金扣除 ¥ 180.00，生成消费流水号并与账单绑定。</text>
          <text x="14" y="86" class="font-sans" font-size="11" fill="#7A6856">3. 生成治疗师王春兰的已完成推拿诊疗记录，可供随诊追踪。</text>
        </g>

        <!-- Final Confirm Button -->
        <g transform="translate(20, 580)">
          <rect x="0" y="0" width="390" height="48" rx="8" fill="url(#primaryGrad)" class="shadow-md"/>
          <text x="195" y="30" class="font-sans" font-size="15" font-weight="700" fill="#FFFFFF" text-anchor="middle">确认收款并完成收费 (¥ 180.00)</text>

          <!-- Secondary Print Button -->
          <g transform="translate(0, 58)">
            <rect x="0" y="0" width="390" height="34" rx="6" fill="#F0F5F8" stroke="#D3E2EA" stroke-width="1"/>
            <text x="195" y="22" class="font-sans" font-size="12" font-weight="600" fill="#4B687A" text-anchor="middle">🖨 打印门诊推拿治疗单与收据凭证</text>
          </g>
        </g>
      </g>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

with open("ui_render/03_treatment.svg", "w", encoding="utf-8") as f:
    f.write(build_treatment_view())
print("Treatment view written")
