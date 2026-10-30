# 使用指南

## 1. 引入依赖

```xml
<!-- 纯 Java / 非 Spring -->
<dependency>
  <groupId>tech.muyi</groupId>
  <artifactId>my-tspl-core</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>

<!-- Spring Boot 2.x（传递依赖 core） -->
<dependency>
  <groupId>tech.muyi</groupId>
  <artifactId>my-tspl-starter</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 可选：PNG 点阵预览 -->
<dependency>
  <groupId>tech.muyi</groupId>
  <artifactId>my-tspl-test</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

要求：Java 8+。Starter 面向 Spring Boot **2.x**（`spring.factories`）。

## 2. Builder 构建标签

调用顺序即打印图层顺序。先声明 DPI，再用 `mm()` / `dots()` 换算坐标。

```java
TsplLabel label = TsplLabel.create().dpi(300);

TsplDocument doc = label
    .standardHeaderMm(100, 80)   // SIZE + DIRECTION + REFERENCE + SET* + CLS
    .gapMm(2, 0)                 // 可选：纸缝
    .speed(4)
    .density(8)
    .box(label.dots(20), label.dots(20), label.dots(780), label.dots(600), 3)
    .textBitmap(label.dots(40), label.dots(40), "铁托盘",
            TextStyle.bold(label.dots(48)))
    .image(label.dots(40), label.dots(120), logoBytes,
            ImageOptions.maxWidth(label.dots(200)))
    .qrcode(label.dots(560), label.dots(120),
            QrOptions.of("L", label.dots(8)), traycode)
    .barcode(label.dots(40), label.dots(400), "128",
            label.dots(48), 1, 0, 2, 4, "SKU123")
    .print(1, 1)
    .build();
```

### 元素对照

| 需求 | API | 产出 |
|------|-----|------|
| 机内西文 | `text(...)` | `TEXT` |
| 中文 / 任意字体 | `textBitmap(...)` | 内联 `BITMAP` |
| 直线 / 框 | `line` / `hline` / `vline` / `box` | `LINE` / `BOX` |
| 图片 | `image(...)` + `ImageOptions` | 内联 `BITMAP` |
| 二维码 | `qrcode(...)` + `QrOptions` | `QRCODE` |
| 一维码 | `barcode(...)` | `BARCODE` |
| 未封装指令 | `appendRaw(...)` / `append(TsplCmd)` | 原样写出 |

`image` 支持：`BufferedImage`、`byte[]`、`InputStream`、本地路径、`URL`、http(s) 地址字符串。

### ImageOptions / QrOptions

```java
ImageOptions.maxWidth(200)           // 等比缩放到最大宽
    .threshold(128)                  // 二值阈值
    .dither(true);                   // Floyd–Steinberg 抖动

ImageOptions.maxSize(200, 120).keepAspect(false);  // 拉伸填满

QrOptions.of("M", 6)                 // ECC + cell 宽（点）
    .rotation(90)
    .mode("A")
    .model("M2");
```

### 可读预览

```java
String text = doc.toCommands();          // CRLF 文本；BITMAP 为摘要行
List<String> lines = doc.toCommandLines();
byte[] raw = doc.toBytes();
String b64 = doc.toBase64();
```

## 3. DPI

| DPI | dots/mm | 100mm 边长 |
|-----|---------|------------|
| 203 | 8 | 800 |
| 300 | 12 | 1200 |

同一套按 203dpi 写的点坐标打到 300dpi 机，只约占满幅的 **2/3**。推荐：

```java
TsplLabel label = TsplLabel.create().dpi(300);
int x = label.dots(40);    // 「203 设计稿上的 40 点」→ 当前 DPI
int edge = label.mm(100);  // 毫米 → 点
```

`TsplDpi` 规则：`dpi >= 280` 视为 300，否则 203。文档上的 DPI 元数据供预览推算画布；**不会**改写模板正文里的数字坐标。

## 4. 交付（Sink）

```java
// 仅 Base64（前端连打印机）
List<String> list = TsplClient.withBase64(c -> c.send(doc));

// 短生命周期直连
try (TsplClient c = TsplClient.printer("192.168.1.10", 9100)) {
    c.send(doc);
}

// 双模式：有 IP 则 TCP，同时收集 Base64；host 空白则仅 Base64
List<String> dual = TsplClient.withDual(host, 9100, c -> c.send(doc));
```

也可直接使用 `SocketPrinterSink` / `Base64CollectingSink` / `CompositeSink`。

## 5. 模板

```java
TsplDocument doc = TsplTemplate.of(TsplTemplateSources.classpath("tsc"))
    .load("barcode_label.tspl")
    .dpi(300)
    .bind("traycode", code)
    .bind(map)                              // 批量
    .insertBefore("QRCODE", bitmapCmd)      // 匹配行前插入
    .insertAfter(line -> line.startsWith("CLS"), extraCmd)
    .toDocument();
```

占位符格式：`${word}`（`\w+`）。自定义来源：

```java
TsplTemplateSource db = location -> dao.findByCode(location);

TsplTemplateSource chain = TsplTemplateSources.firstOf(
    TsplTemplateSources.file(Paths.get("/opt/wms/tspl")),
    TsplTemplateSources.classpath("tsc")
);
```

还支持：`url()`、`string(content)`、`map(...)`、`caching(delegate)`。

## 6. Spring Boot（starter）

引入 `my-tspl-starter` 后自动注册 `TsplPrinterManager`。**地址不写死在配置里**，由业务传入：

```java
@Autowired
private TsplPrinterManager printers;

public void print(String host, int port, TsplDocument doc) {
    printers.send(host, port, doc);
    List<String> b64 = printers.sendDual(host, port, doc);
}

// 或自行持有
TsplPrinter p = printers.get(host, port);
p.getLastSendAtMs();  // 最近成功发送时间
printers.evict(host, port);
```

同 host:port 复用一条 TCP；写失败默认可自动重连一次；应用关闭时 manager 统一断开。

## 7. PNG 预览（my-tspl-test）

```java
import tech.muyi.tspl.preview.*;

TsplPreview.toPng(doc, PreviewPaths.file("label.png"),
    ImagePreviewOptions.defaults().zoom(3).roundDots(true));
```

能力边界：

- 支持：`BITMAP`、`LINE`/`BOX`、真实 `QRCODE`（ZXing）
- 占位：机内 `TEXT`、`BARCODE`

无头 Linux 渲染中文时，请在 classpath 放置 `tspl/label_font.ttf`（或 `tsc/`、`.otf`），或安装系统 CJK 字体。

## 8. 案例与实机调试

案例预览图见 [docs/examples](examples/README.md)（`docs/examples/images/` 纳入 Git）。源码：`AllInOneLabelTest`、`LabelStyleScenariosTest`。

```bash
# 跑场景并刷新版本库图片
./scripts/refresh-example-images.sh

# 或单独跑综合样张
mvn -pl my-tspl-test -am test -Dtest=AllInOneLabelTest
```

场景测试默认**不连打印机**。指定地址后才会下发：

```bash
mvn -pl my-tspl-test -am test -Dtest=AllInOneLabelTest \
  -Dtspl.printer.host=192.168.1.10 \
  -Dtspl.printer.dpi=300
```

环境变量：`TSPL_PRINTER_HOST` / `TSPL_PRINTER_PORT` / `TSPL_PRINTER_DPI`。
