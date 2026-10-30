# my-tspl

面向 TSC / 兼容机的 **TSPL 标签指令库**：服务端拼装指令，既可 TCP 直连打印机，也可返回 Base64 给前端 PrintServer。

```
TsplLabel / TsplTemplate  →  TsplDocument  →  byte[]  →  Socket | Base64 | Dual
```

## 模块

| 模块 | 说明 |
|------|------|
| [`my-tspl-core`](my-tspl-core) | 零框架核心：Builder / 模板 / 编码 / Sink |
| [`my-tspl-starter`](my-tspl-starter) | Spring Boot 2.x：按 host:port 缓存长连接 |
| [`my-tspl-test`](my-tspl-test) | PNG 点阵预览（ZXing）+ 场景案例测试 |

```xml
<dependency>
  <groupId>tech.muyi</groupId>
  <artifactId>my-tspl-core</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>

<!-- Spring Boot（已传递 core） -->
<dependency>
  <groupId>tech.muyi</groupId>
  <artifactId>my-tspl-starter</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Java 8+ · 坐标单位：**点** · 默认字符集：**GB2312**

## 案例预览

每个案例均为 **代码 → 效果图**；完整图库见 **[docs/examples](docs/examples/README.md)**。刷新：`./scripts/refresh-example-images.sh`

### WMS 托盘标签

```java
TsplDocument doc = TsplLabel.create()
    .standardHeaderMm(100, 80)
    .box(20, 20, 780, 620, 3)
    .hline(20, 100, 760, 2)
    .textBitmap(40, 40, "铁托盘", TextStyle.bold(48))
    .textBitmap(40, 130, "托盘号", TextStyle.bold(22))
    .textBitmap(40, 170, "B3-8899", TextStyle.bold(72))
    .textBitmap(40, 310, "库位", TextStyle.bold(22))
    .textBitmap(40, 350, "A-12-03", TextStyle.plain(40))
    .qrcode(560, 320, QrOptions.of("L", 10), "B3-8899")
    .print(1)
    .build();
```

![tray](docs/examples/images/05-tray-label.png)

### 快递面单

```java
TsplDocument doc = TsplLabel.create()
    .standardHeaderMm(100, 70)
    .box(16, 16, 784, 544, 3)
    .textBitmapCentered(36, "快递面单 / SHIPPING", TextStyle.bold(40), 800)
    .textBitmap(32, 155, "张三  138****0000", TextStyle.bold(32))
    .textBitmap(32, 245, "浙江省杭州市余杭区文一西路xxx号",
            TextStyle.plain(26).maxWidth(480))
    .qrcode(560, 130, QrOptions.of("M", 7), "SF1234567890")
    .barcode(40, 450, "128", 70, 1, 0, 2, 4, "SF1234567890")
    .print(1)
    .build();
```

![shipping](docs/examples/images/06-shipping-label.png)

更多（文字样式 / 表格 / 价签 / 综合样张等）：[案例图库](docs/examples/README.md)

## 文档

| 文档 | 内容 |
|------|------|
| [使用指南](docs/guide.md) | 依赖、Builder、DPI、交付、模板、Spring、预览 |
| [架构说明](docs/architecture.md) | 流水线、模块、编码约定 |
| [API 速查](docs/api.md) | 主要类与方法表 |
| [案例图库](docs/examples/README.md) | 代码示例 + 预览 PNG（`AllInOneLabelTest` / `LabelStyleScenariosTest`） |

## 特性

- **流式 Builder**（`TsplLabel`）：SIZE / LINE / BOX / TEXT / QRCODE / 图片 BITMAP / PRINT…
- **原子指令**（`TsplCmds`）：可单测、可 `append` 扩展
- **可插拔模板**（`TsplTemplateSource`）：classpath / 文件 / URL / Map / 自定义（DB、OSS）
- **双交付**（`TsplClient`）：`base64()` / `printer()` / `dual()`
- **中文与图片 BITMAP**：服务端 AWT → 内联位图（机内字体通常无中文）
- **DPI 换算**：`label.mm()` / `label.dots()`，预览按文档 DPI 推算画布
- **Starter 长连接**：`TsplPrinterManager` 按地址缓存 TCP，写失败可自动重连

## 30 秒上手

```java
TsplLabel label = TsplLabel.create().dpi(300);
TsplDocument doc = label
    .standardHeaderMm(100, 80)
    .box(label.dots(20), label.dots(20), label.dots(780), label.dots(600), 3)
    .textBitmap(label.dots(40), label.dots(40), "铁托盘",
            TextStyle.bold(label.dots(48)))
    .qrcode(label.dots(560), label.dots(80),
            QrOptions.of("L", label.dots(8)), "B3-8899")
    .print(1, 1)
    .build();

// 返回前端
List<String> base64List = TsplClient.withBase64(c -> c.send(doc));

// 或直连打印机
try (TsplClient client = TsplClient.printer("192.168.1.10", 9100)) {
    client.send(doc);
}
```

可读命令：`doc.toCommands()`。完整「代码 + 效果图」见 [案例图库](docs/examples/README.md)。PNG 预览（依赖 `my-tspl-test`）：

```java
TsplPreview.toPng(doc, PreviewPaths.file("label.png"),
    ImagePreviewOptions.defaults().zoom(3));
```

## 模板

```java
TsplDocument doc = TsplTemplate.of(TsplTemplateSources.classpath("tsc"))
    .load("barcode_label.tspl")
    .bind("traycode", code)
    .insertBefore("QRCODE",
        TextBitmapRenderer.command(50, 30, "铁托盘", TextStyle.bold(48)))
    .toDocument();
```

自定义来源与回退链见 [使用指南 · 模板](docs/guide.md#5-模板)。

## Spring Boot

引入 starter 后注入 `TsplPrinterManager`，打印机地址由调用方传入（请求 / DB / 配置中心均可）：

```java
@Autowired
private TsplPrinterManager printers;

public void printLabel(String host, int port, TsplDocument doc) {
    printers.send(host, port, doc);
    List<String> b64 = printers.sendDual(host, port, doc);
}
```

## DPI 要点

| DPI | dots/mm | 100mm 边长 |
|-----|---------|------------|
| 203 | 8 | 800 |
| 300 | 12 | 1200 |

按 203 设计的点坐标打到 300dpi 机只约占满幅 **2/3**。先 `dpi(300)`，再用 `dots(n)` / `mm(n)` 换算。

## 运行测试与案例图

```bash
# 单元测试
mvn test

# 综合样张 + 样式场景（默认不连打印机）
mvn -pl my-tspl-test -am test \
  -Dtest=AllInOneLabelTest,LabelStyleScenariosTest

# 可选实机
mvn -pl my-tspl-test -am test -Dtest=AllInOneLabelTest \
  -Dtspl.printer.host=192.168.1.10 -Dtspl.printer.dpi=300

# 刷新版本库内案例图 docs/examples/images/
./scripts/refresh-example-images.sh
```

## 包一览

| 包 | 模块 | 职责 |
|----|------|------|
| `builder` | core | `TsplLabel` / `QrOptions` / `ImageOptions` |
| `cmd` | core | 原子指令 |
| `template` | core | 模板绑定与 Source |
| `render` | core | 文字 / 图片 → BITMAP |
| `sink` | core | Socket / Base64 / Composite / `TsplClient` |
| `encode` / `io` | core | Document → bytes |
| `boot` | starter | `TsplPrinter` / `TsplPrinterManager` |
| `preview` | test | `TsplPreview` 点阵 PNG |

## 注意事项

- 无头环境渲染中文：请提供 classpath 字体 `tspl/label_font.ttf`（或系统 CJK 字体）。
- 预览模块对机内 `TEXT` / `BARCODE` 仅占位；二维码为真实图案。
- 未封装的 TSPL 指令可通过 `appendRaw` / `TsplCmds.raw` 透传。
- Starter 当前为 Boot 2.x 自动配置；打印机超时 / 重连参数在构造 `TsplPrinterManager` 时传入，无 `application.yml` 绑定。
