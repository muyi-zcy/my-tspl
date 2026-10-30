# 架构

## 设计原则

1. **造指令与交付分离**：先得到不可变的 `TsplDocument`，再选择 Socket / Base64 / 双模式下发。
2. **坐标是点，尺寸是毫米**：`SIZE` 用 mm；`BOX` / `TEXT` / `QRCODE` / `BITMAP` 的 x,y 与宽高用点。DPI 只影响换算与预览画布。
3. **中文走服务端 BITMAP**：多数 TSC 机内字体不含中文；`textBitmap` / `image` 在 JVM 内用 AWT 渲成单色位图再内联。
4. **模板可插拔**：`TsplTemplateSource` 可接 classpath、文件、URL、DB、OSS。

## 流水线

```
TsplLabel  /  TsplTemplate.Bound
              │
              ▼
        TsplDocument
     (cmds + charset + dpi)
              │
              ▼
         TsplEncoder
              │
              ▼
           byte[]
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
 Socket   Base64    Composite
 Printer  Collect   (dual)
```

## 模块依赖

```
my-tspl-test ──► my-tspl-core
                      │
my-tspl-starter ──────┘
```

| 模块 | 运行时依赖 | 用途 |
|------|------------|------|
| `my-tspl-core` | 无 | 构建 / 模板 / 编码 / Sink |
| `my-tspl-starter` | core + Spring Boot | 长连接管理 |
| `my-tspl-test` | core + ZXing | PNG 预览 + 场景案例 |

## 包职责（core）

| 包 | 职责 |
|----|------|
| `builder` | `TsplLabel` 流式 API |
| `cmd` | 原子指令与工厂 |
| `template` | `${placeholder}`、行钩子、Source |
| `render` | 文字 / 图片 → 内联 BITMAP |
| `encode` / `io` | Document → bytes |
| `sink` | 交付通道与 `TsplClient` |

## 编码约定

- 默认字符集 **GB2312**（常见 TSC 固件）；可用 `charset(...)` 覆盖。
- `BITMAP` / `DOWNLOAD` 头行强制 ASCII；载荷为原始字节。
- 行结束符为 **CRLF**（`\r\n`）。
- 单色位图：bit0 = 打印黑点，MSB first，宽度以字节计。
