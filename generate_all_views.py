# -*- coding: utf-8 -*-
import subprocess
import os

svg_header = """<svg width="1440" height="900" viewBox="0 0 1440 900" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <style>
      .font-sans { font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Segoe UI", sans-serif; }
      .shadow-sm { filter: drop-shadow(0 1px 2px rgba(0,0,0,0.04)); }
      .shadow-md { filter: drop-shadow(0 4px 12px rgba(16,40,32,0.06)); }
      .shadow-lg { filter: drop-shadow(0 10px 25px rgba(16,40,32,0.08)); }
    </style>
    <linearGradient id="primaryGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#087F6D" />
      <stop offset="100%" stop-color="#149A84" />
    </linearGradient>
    <linearGradient id="primaryCardGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="100%" stop-color="#F2F9F7" />
    </linearGradient>
    <linearGradient id="warmCardGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="100%" stop-color="#FFFBF5" />
    </linearGradient>
    <linearGradient id="blueCardGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="100%" stop-color="#F4F8FC" />
    </linearGradient>
  </defs>

  <!-- Background Canvas -->
  <rect width="1440" height="900" fill="#F4F7F9" />
"""

def make_shell(active_nav="dashboard", user_name="李医生 (主任治疗师)", clinic_name="和康堂中医推拿门诊"):
    # Window Top Bar (Desktop Titlebar)
    svg = f"""
  <!-- macOS/Windows Window Frame & Header -->
  <rect x="0" y="0" width="1440" height="64" fill="#FFFFFF" class="shadow-sm"/>
  <line x1="0" y1="64" x2="1440" y2="64" stroke="#E5EDF0" stroke-width="1"/>

  <!-- Window controls -->
  <circle cx="24" cy="32" r="6" fill="#FF5F56"/>
  <circle cx="44" cy="32" r="6" fill="#FFBD2E"/>
  <circle cx="64" cy="32" r="6" fill="#27C93F"/>

  <!-- Brand Logo & Clinic Info -->
  <g transform="translate(100, 14)">
    <rect x="0" y="0" width="36" height="36" rx="9" fill="url(#primaryGrad)"/>
    <path d="M18 10 V26 M10 18 H26" stroke="#FFFFFF" stroke-width="3.2" stroke-linecap="round"/>
    <text x="46" y="22" class="font-sans" font-size="18" font-weight="700" fill="#13334A">HealTouch</text>
    <text x="142" y="22" class="font-sans" font-size="12" font-weight="500" fill="#087F6D" letter-spacing="1">门诊专业版 V2.4</text>
    <text x="46" y="34" class="font-sans" font-size="11" fill="#7892A0">{clinic_name}</text>
  </g>

  <!-- Top Right Operator Info -->
  <g transform="translate(1120, 16)">
    <rect x="0" y="0" width="200" height="32" rx="16" fill="#F0F5F8"/>
    <circle cx="16" cy="16" r="11" fill="#087F6D"/>
    <text x="16" y="20" class="font-sans" font-size="11" font-weight="600" fill="#FFFFFF" text-anchor="middle">李</text>
    <text x="36" y="20" class="font-sans" font-size="12" font-weight="600" fill="#244255">{user_name}</text>
    <rect x="215" y="0" width="80" height="32" rx="6" fill="#FFFFFF" stroke="#D3E0E8" stroke-width="1"/>
    <text x="255" y="20" class="font-sans" font-size="12" font-weight="600" fill="#668292" text-anchor="middle">退出登录</text>
  </g>

  <!-- Left Sidebar Navigation -->
  <rect x="0" y="64" width="220" height="836" fill="#FFFFFF"/>
  <line x1="220" y1="64" x2="220" y2="900" stroke="#E5EDF0" stroke-width="1"/>
"""
    nav_items = [
        ("dashboard", "首页工作台", "01"),
        ("patients", "患者档案", "02"),
        ("treatment", "治疗登记", "03"),
        ("bills", "账单与收费", "04"),
        ("refunds", "退款审批", "05"),
        ("deposits", "预存管理", "06"),
        ("reports", "数据统计", "07"),
    ]
    system_items = [
        ("users", "用户管理"),
        ("catalog", "治疗项目"),
        ("permissions", "权限管理"),
    ]

    svg += """
  <text x="24" y="96" class="font-sans" font-size="11" font-weight="700" fill="#8DA4B0" letter-spacing="0.5">核心门诊业务</text>
"""
    y = 110
    for key, label, num in nav_items:
        is_active = (key == active_nav)
        bg = 'fill="#E8F6F3"' if is_active else 'fill="transparent"'
        text_fill = '#087F6D' if is_active else '#415E70'
        font_wt = '700' if is_active else '500'
        badge = f'<rect x="216" y="{y}" width="4" height="40" rx="2" fill="#087F6D"/>' if is_active else ''
        svg += f"""
  <g transform="translate(12, {y})">
    <rect x="0" y="0" width="196" height="40" rx="8" {bg}/>
    <circle cx="20" cy="20" r="10" fill="{'#C8EBE3' if is_active else '#F2F6F8'}"/>
    <text x="20" y="24" class="font-sans" font-size="10" font-weight="700" fill="{'#087F6D' if is_active else '#829BA8'}" text-anchor="middle">{num}</text>
    <text x="40" y="25" class="font-sans" font-size="14" font-weight="{font_wt}" fill="{text_fill}">{label}</text>
  </g>
  {badge}
"""
        y += 46

    # Separator
    y += 10
    svg += f"""
  <line x1="24" y1="{y}" x2="196" y2="{y}" stroke="#EDF2F5" stroke-width="1"/>
  <text x="24" y="{y+24}" class="font-sans" font-size="11" font-weight="700" fill="#8DA4B0" letter-spacing="0.5">门诊运维设置</text>
"""
    y += 36
    for key, label in system_items:
        is_active = (key == active_nav)
        bg = 'fill="#E8F6F3"' if is_active else 'fill="transparent"'
        text_fill = '#087F6D' if is_active else '#587383'
        font_wt = '700' if is_active else '500'
        svg += f"""
  <g transform="translate(12, {y})">
    <rect x="0" y="0" width="196" height="38" rx="8" {bg}/>
    <text x="40" y="24" class="font-sans" font-size="13" font-weight="{font_wt}" fill="{text_fill}">{label}</text>
  </g>
"""
        y += 42

    # Bottom status in sidebar
    svg += """
  <g transform="translate(16, 840)">
    <rect x="0" y="0" width="188" height="44" rx="8" fill="#F7FAFC" stroke="#E3EBEF" stroke-width="1"/>
    <circle cx="16" cy="22" r="4" fill="#10B981"/>
    <text x="28" y="20" class="font-sans" font-size="11" font-weight="600" fill="#2E4A5C">本地 SQLite 数据库在线</text>
    <text x="28" y="34" class="font-sans" font-size="10" fill="#8CA1AE">就诊记录实时双写保障</text>
  </g>
"""
    return svg

print("Shell template ready")
