# -*- coding: utf-8 -*-
import subprocess
from generate_all_views import svg_header, make_shell

def build_patient_view():
    shell = make_shell(active_nav="patients")
    content = """
  <!-- Main Patient Management Area -->
  <g transform="translate(252, 92)">
    <!-- Header Title & Action Buttons -->
    <text x="0" y="24" class="font-sans" font-size="24" font-weight="700" fill="#13334A">患者档案管理</text>
    <text x="0" y="46" class="font-sans" font-size="13" fill="#668292">建立规范门诊健康档案 · 支持成人与小儿患者特殊监护人必填及重复筛查</text>

    <g transform="translate(930, 10)">
      <rect x="0" y="0" width="160" height="40" rx="8" fill="url(#primaryGrad)"/>
      <text x="80" y="25" class="font-sans" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle">＋ 创建新患者档案</text>
    </g>

    <!-- Filter & Search Toolbar -->
    <g transform="translate(0, 68)">
      <rect x="0" y="0" width="1150" height="64" rx="12" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
      
      <!-- Search Input -->
      <g transform="translate(20, 14)">
        <rect x="0" y="0" width="280" height="36" rx="6" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
        <text x="14" y="23" class="font-sans" font-size="12" fill="#8CA4B2">搜索姓名 / 证件号 / 手机号 / 档案号</text>
      </g>

      <!-- Patient Type Dropdown -->
      <g transform="translate(315, 14)">
        <rect x="0" y="0" width="140" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
        <text x="14" y="23" class="font-sans" font-size="12" fill="#2C485A">患者类型: 全部</text>
        <path d="M120 16 L125 21 L130 16" stroke="#8CA4B2" stroke-width="1.5" fill="none"/>
      </g>

      <!-- Date Filter -->
      <g transform="translate(470, 14)">
        <rect x="0" y="0" width="180" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
        <text x="14" y="23" class="font-sans" font-size="12" fill="#2C485A">建档日期: 本月全部</text>
        <path d="M160 16 L165 21 L170 16" stroke="#8CA4B2" stroke-width="1.5" fill="none"/>
      </g>

      <!-- Search Buttons -->
      <g transform="translate(665, 14)">
        <rect x="0" y="0" width="80" height="36" rx="6" fill="#087F6D"/>
        <text x="40" y="23" class="font-sans" font-size="12" font-weight="600" fill="#FFFFFF" text-anchor="middle">查询</text>

        <rect x="90" y="0" width="80" height="36" rx="6" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
        <text x="130" y="23" class="font-sans" font-size="12" font-weight="600" fill="#5A7686" text-anchor="middle">重置</text>
      </g>

      <!-- Right Export Button -->
      <g transform="translate(1010, 14)">
        <rect x="0" y="0" width="120" height="36" rx="6" fill="#F8FAFC" stroke="#D3E2EA" stroke-width="1"/>
        <text x="60" y="23" class="font-sans" font-size="12" font-weight="600" fill="#4B6676" text-anchor="middle">⬇ 导出档案(脱敏)</text>
      </g>
    </g>

    <!-- Patient Master-Detail View: Left Table (720px), Right Detail & History Card (410px) -->
    <g transform="translate(0, 148)">
      <!-- Left List Container -->
      <g transform="translate(0, 0)">
        <rect x="0" y="0" width="720" height="620" rx="14" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        
        <!-- Table Header -->
        <rect x="16" y="16" width="688" height="38" rx="6" fill="#EDF6F4"/>
        <text x="32" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">档案编号</text>
        <text x="120" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">姓名 / 性别</text>
        <text x="230" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">患者类型</text>
        <text x="310" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">联系电话</text>
        <text x="440" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">证件号码 (已脱敏)</text>
        <text x="600" y="40" class="font-sans" font-size="12" font-weight="700" fill="#2A5364">预存余额</text>

        <!-- Selected Active Row: P000109 刘浩宇 (小儿) -->
        <rect x="16" y="58" width="688" height="52" rx="8" fill="#F0F8F6" stroke="#A9DDD1" stroke-width="1.5"/>
        <text x="32" y="89" class="font-sans" font-size="12" font-weight="700" fill="#087F6D">P000109</text>
        <text x="120" y="89" class="font-sans" font-size="13" font-weight="700" fill="#13334A">刘浩宇 <tspan font-size="11" font-weight="400" fill="#668292">(男 5岁)</tspan></text>
        <rect x="230" y="74" width="48" height="20" rx="4" fill="#FEF2E2"/>
        <text x="238" y="88" class="font-sans" font-size="11" font-weight="600" fill="#C26A08">小儿</text>
        <text x="310" y="89" class="font-sans" font-size="12" fill="#264455">138****6621</text>
        <text x="440" y="89" class="font-sans" font-size="12" fill="#587383">出生证 5101****89</text>
        <text x="600" y="89" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 1,480.00</text>

        <!-- Row 2: 张建国 -->
        <line x1="16" y1="164" x2="704" y2="164" stroke="#F0F4F7" stroke-width="1"/>
        <text x="32" y="142" class="font-sans" font-size="12" fill="#668292">P000108</text>
        <text x="120" y="142" class="font-sans" font-size="13" font-weight="700" fill="#13334A">张建国 <tspan font-size="11" font-weight="400" fill="#668292">(男 46岁)</tspan></text>
        <rect x="230" y="128" width="48" height="20" rx="4" fill="#E7F3F9"/>
        <text x="238" y="142" class="font-sans" font-size="11" font-weight="600" fill="#1E6589">成人</text>
        <text x="310" y="142" class="font-sans" font-size="12" fill="#264455">139****1120</text>
        <text x="440" y="142" class="font-sans" font-size="12" fill="#587383">身份证 110101****3211</text>
        <text x="600" y="142" class="font-sans" font-size="13" font-weight="700" fill="#1A384A">¥ 350.00</text>

        <!-- Row 3: 陈玉玲 -->
        <line x1="16" y1="218" x2="704" y2="218" stroke="#F0F4F7" stroke-width="1"/>
        <text x="32" y="196" class="font-sans" font-size="12" fill="#668292">P000094</text>
        <text x="120" y="196" class="font-sans" font-size="13" font-weight="700" fill="#13334A">陈玉玲 <tspan font-size="11" font-weight="400" fill="#668292">(女 39岁)</tspan></text>
        <rect x="230" y="182" width="48" height="20" rx="4" fill="#E7F3F9"/>
        <text x="238" y="196" class="font-sans" font-size="11" font-weight="600" fill="#1E6589">成人</text>
        <text x="310" y="196" class="font-sans" font-size="12" fill="#264455">136****5409</text>
        <text x="440" y="196" class="font-sans" font-size="12" fill="#587383">身份证 320102****6624</text>
        <text x="600" y="196" class="font-sans" font-size="13" font-weight="700" fill="#1A384A">¥ 0.00</text>

        <!-- Row 4: 赵文博 -->
        <line x1="16" y1="272" x2="704" y2="272" stroke="#F0F4F7" stroke-width="1"/>
        <text x="32" y="250" class="font-sans" font-size="12" fill="#668292">P000082</text>
        <text x="120" y="250" class="font-sans" font-size="13" font-weight="700" fill="#13334A">赵文博 <tspan font-size="11" font-weight="400" fill="#668292">(男 52岁)</tspan></text>
        <rect x="230" y="236" width="48" height="20" rx="4" fill="#E7F3F9"/>
        <text x="238" y="250" class="font-sans" font-size="11" font-weight="600" fill="#1E6589">成人</text>
        <text x="310" y="250" class="font-sans" font-size="12" fill="#264455">188****8833</text>
        <text x="440" y="250" class="font-sans" font-size="12" fill="#587383">身份证 310104****1015</text>
        <text x="600" y="250" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 2,100.00</text>

        <!-- Row 5: 孙思萌 (小儿) -->
        <line x1="16" y1="326" x2="704" y2="326" stroke="#F0F4F7" stroke-width="1"/>
        <text x="32" y="304" class="font-sans" font-size="12" fill="#668292">P000110</text>
        <text x="120" y="304" class="font-sans" font-size="13" font-weight="700" fill="#13334A">孙思萌 <tspan font-size="11" font-weight="400" fill="#668292">(女 3岁)</tspan></text>
        <rect x="230" y="290" width="48" height="20" rx="4" fill="#FEF2E2"/>
        <text x="238" y="304" class="font-sans" font-size="11" font-weight="600" fill="#C26A08">小儿</text>
        <text x="310" y="304" class="font-sans" font-size="12" fill="#264455">137****9088</text>
        <text x="440" y="304" class="font-sans" font-size="12" fill="#587383">户口本 320501****22</text>
        <text x="600" y="304" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">¥ 600.00</text>

        <!-- Pagination Bar -->
        <g transform="translate(24, 575)">
          <text x="0" y="16" class="font-sans" font-size="12" fill="#758E9D">显示 1 至 5 条，共 142 位档案</text>
          <g transform="translate(480, 0)">
            <rect x="0" y="0" width="60" height="28" rx="4" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="30" y="18" class="font-sans" font-size="11" fill="#668292" text-anchor="middle">上一页</text>
            <rect x="70" y="0" width="28" height="28" rx="4" fill="#087F6D"/>
            <text x="84" y="18" class="font-sans" font-size="11" font-weight="700" fill="#FFFFFF" text-anchor="middle">1</text>
            <rect x="105" y="0" width="28" height="28" rx="4" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="119" y="18" class="font-sans" font-size="11" fill="#668292" text-anchor="middle">2</text>
            <rect x="140" y="0" width="60" height="28" rx="4" fill="#FFFFFF" stroke="#D3E2EA" stroke-width="1"/>
            <text x="170" y="18" class="font-sans" font-size="11" fill="#668292" text-anchor="middle">下一页</text>
          </g>
        </g>
      </g>

      <!-- Right Detail Box: 详细信息卡片 & 小儿监护人规则 & 历史就诊记录 -->
      <g transform="translate(740, 0)">
        <rect x="0" y="0" width="410" height="620" rx="14" fill="#FFFFFF" stroke="#E1EBF0" stroke-width="1" class="shadow-sm"/>
        
        <!-- Detail Header -->
        <g transform="translate(20, 20)">
          <text x="0" y="18" class="font-sans" font-size="17" font-weight="700" fill="#13334A">患者档案详情</text>
          <rect x="290" y="0" width="80" height="28" rx="5" fill="#F0F7F6" stroke="#BCE3DA" stroke-width="1"/>
          <text x="330" y="18" class="font-sans" font-size="11" font-weight="700" fill="#087F6D" text-anchor="middle">修改资料</text>
        </g>

        <!-- Patient Highlight Banner -->
        <g transform="translate(20, 56)">
          <rect x="0" y="0" width="370" height="74" rx="10" fill="url(#warmCardGrad)" stroke="#F3E5D0" stroke-width="1"/>
          <circle cx="28" cy="37" r="18" fill="#FEE7C8"/>
          <text x="28" y="42" class="font-sans" font-size="13" font-weight="700" fill="#B45309" text-anchor="middle">儿</text>
          <text x="56" y="30" class="font-sans" font-size="16" font-weight="700" fill="#13334A">刘浩宇</text>
          <text x="110" y="30" class="font-sans" font-size="12" fill="#758E9D">#P000109 · 男 · 5岁</text>
          <text x="56" y="52" class="font-sans" font-size="12" fill="#5A748A">预存金可用余额: <tspan font-weight="700" fill="#087F6D">¥ 1,480.00</tspan></text>
          
          <rect x="270" y="22" width="88" height="30" rx="6" fill="#087F6D"/>
          <text x="314" y="41" class="font-sans" font-size="11" font-weight="700" fill="#FFFFFF" text-anchor="middle">⚡ 发起治疗</text>
        </g>

        <!-- Information Fields Grid -->
        <g transform="translate(20, 142)">
          <!-- Field 1 -->
          <text x="0" y="16" class="font-sans" font-size="12" fill="#7893A2">证件类型/号:</text>
          <text x="90" y="16" class="font-sans" font-size="12" font-weight="600" fill="#1A384A">出生医学证明 (510104****89)</text>

          <!-- Field 2 -->
          <text x="0" y="40" class="font-sans" font-size="12" fill="#7893A2">出生日期:</text>
          <text x="90" y="40" class="font-sans" font-size="12" fill="#1A384A">2021-04-15 (5周岁)</text>

          <!-- Field 3 小儿监护人重点提示框 -->
          <g transform="translate(0, 54)">
            <rect x="0" y="0" width="370" height="66" rx="8" fill="#F4F8FA" stroke="#D3E2EA" stroke-width="1"/>
            <rect x="10" y="10" width="76" height="20" rx="4" fill="#087F6D"/>
            <text x="48" y="24" class="font-sans" font-size="10" font-weight="700" fill="#FFFFFF" text-anchor="middle">小儿法定监护</text>
            <text x="94" y="25" class="font-sans" font-size="12" font-weight="700" fill="#16384C">刘建民 (父子关系)</text>
            <text x="10" y="48" class="font-sans" font-size="11" fill="#668292">监护人手机号: <tspan font-weight="600" fill="#1A384A">13800136621</tspan> · 随访已验证</text>
          </g>

          <!-- Field 4 既往史 & 过敏史 -->
          <g transform="translate(0, 130)">
            <text x="0" y="16" class="font-sans" font-size="12" fill="#7893A2">药物过敏史:</text>
            <text x="90" y="16" class="font-sans" font-size="12" font-weight="600" fill="#DC2626">头孢类药物过敏；青霉素皮试阴性</text>

            <text x="0" y="40" class="font-sans" font-size="12" fill="#7893A2">既往调理史:</text>
            <text x="90" y="40" class="font-sans" font-size="12" fill="#1A384A">自幼脾胃虚弱，易积食发热，定期小儿推拿</text>

            <text x="0" y="64" class="font-sans" font-size="12" fill="#7893A2">居住地址:</text>
            <text x="90" y="64" class="font-sans" font-size="12" fill="#1A384A">成都市高新区锦城大道888号</text>
          </g>
        </g>

        <!-- History Services Tabs & Small List -->
        <g transform="translate(20, 360)">
          <line x1="0" y1="0" x2="370" y2="0" stroke="#EEF3F6" stroke-width="1"/>
          
          <g transform="translate(0, 16)">
            <text x="0" y="16" class="font-sans" font-size="13" font-weight="700" fill="#087F6D">历史治疗记录 (共4次)</text>
            <text x="170" y="16" class="font-sans" font-size="12" fill="#758E9D">历史账单</text>
            <text x="250" y="16" class="font-sans" font-size="12" fill="#758E9D">预存流水</text>
          </g>
          
          <!-- History Item 1 -->
          <g transform="translate(0, 48)">
            <rect x="0" y="0" width="370" height="58" rx="8" fill="#F8FAFC" stroke="#E3EBF0" stroke-width="1"/>
            <text x="12" y="24" class="font-sans" font-size="12" font-weight="700" fill="#13334A">小儿积食调理推拿</text>
            <text x="140" y="24" class="font-sans" font-size="11" fill="#7E96A4">2026-10-18 11:00 · 王春兰</text>
            <text x="310" y="24" class="font-sans" font-size="12" font-weight="700" fill="#13334A">¥ 120.00</text>
            <text x="12" y="44" class="font-sans" font-size="11" fill="#728B99">账单 #B20261018-002 · 预存扣款结算 · 恢复良好</text>
          </g>

          <!-- History Item 2 -->
          <g transform="translate(0, 114)">
            <rect x="0" y="0" width="370" height="58" rx="8" fill="#F8FAFC" stroke="#E3EBF0" stroke-width="1"/>
            <text x="12" y="24" class="font-sans" font-size="12" font-weight="700" fill="#13334A">小儿健脾和胃推拿</text>
            <text x="140" y="24" class="font-sans" font-size="11" fill="#7E96A4">2026-10-12 15:30 · 王春兰</text>
            <text x="310" y="24" class="font-sans" font-size="12" font-weight="700" fill="#13334A">¥ 120.00</text>
            <text x="12" y="44" class="font-sans" font-size="11" fill="#728B99">账单 #B20261012-005 · 预存扣款结算 · 消化吸收好转</text>
          </g>
        </g>
      </g>
    </g>
  </g>
"""
    return svg_header + shell + content + "\n</svg>"

with open("ui_render/02_patients.svg", "w", encoding="utf-8") as f:
    f.write(build_patient_view())
print("Patient view written")
