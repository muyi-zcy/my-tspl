# API 速查

## TsplLabel

| 方法 | 说明 |
|------|------|
| `create()` | 新建 Builder，默认 GB2312 / 203dpi |
| `charset(Charset)` / `dpi(int\|TsplDpi)` | 元数据 |
| `mm(double)` / `dots(int)` | 毫米→点；203 设计点→当前 DPI |
| `standardHeaderMm(w,h)` | SIZE + DIRECTION + REFERENCE + OFFSET + SET* + CLS |
| `cls` / `sizeMm` / `sizeInch` / `gapMm` | 页设置 |
| `direction` / `reference` / `offsetMm` | 原点与方向 |
| `peel` / `cutter` / `partialCutter` / `tear` | SET 开关 |
| `speed` / `density` | 速度 / 浓度 |
| `text` / `textBitmap` / `textBitmapCentered` | 文字 |
| `line` / `hline` / `vline` / `box` | 矢量线框 |
| `qrcode` / `barcode` | 码 |
| `image(...)` / `bitmap` / `downloadFile` | 图 / 原始位图 / 下载文件 |
| `print(sets[, copies])` | PRINT |
| `append` / `appendRaw` | 扩展 |
| `build()` / `toBytes()` / `toBase64()` | 产出 |

## TsplDocument

| 方法 | 说明 |
|------|------|
| `builder()` | 手动组装 |
| `commands()` / `charset()` / `dpi()` | 只读 |
| `toBytes()` / `toBase64()` | 编码 |
| `toCommandLines()` / `toCommands()` | 可读文本（二进制指令为摘要） |
| `writeTo(TsplOutput)` | 底层写出 |

## TsplCmds

静态工厂：`cls`、`sizeMm`/`sizeInch`、`gapMm`、`direction`、`reference`、`offsetMm`、`setPeel`/`setCutter`/`setPartialCutter`/`setTear`、`set`/`setFlag`、`speed`、`density`、`qrcode`、`barcode`、`text`、`line`、`box`、`hline`、`vline`、`print`、`bitmap`、`downloadFile`、`raw`、`rawBytes`。

## TsplClient / Sink

| API | 说明 |
|-----|------|
| `base64()` | 仅收集 Base64 |
| `printer(host, port[, timeout])` | TCP |
| `dual(host, port[, timeout])` | Composite；host 空则仅 Base64 |
| `withBase64` / `withDual` | try-with-resources 风格 |
| `send` / `sendAll` / `drainBase64` / `peekBase64` | 收发 |

底层：`SocketPrinterSink`、`Base64CollectingSink`、`CompositeSink`、`TsplSink`。

## 模板

| API | 说明 |
|-----|------|
| `TsplTemplate.of(source).load(loc)` | 加载 → `Bound` |
| `fromContent(text)` | 跳过 Source |
| `Bound.bind` / `dpi` / `charset` | 绑定与元数据 |
| `insertBefore` / `insertAfter` | 行钩子（字符串前缀或 `Predicate`） |
| `toDocument` / `toBytes` / `toBase64` / `filledText` | 产出 |

`TsplTemplateSources`：`classpath`、`file`、`url`、`string`、`map`、`caching`、`firstOf`。

## 渲染

| API | 说明 |
|-----|------|
| `TextStyle.bold/plain(size)` | 字号；可链式 `maxWidth` / `bitmapMode` |
| `TextBitmapRenderer.command/centered` | 文字 → BITMAP 指令 |
| `ImageBitmapRenderer.command(...)` | 图片 → BITMAP |
| `MonoBitmap.pack` | BufferedImage → 单色打包 |

## Spring Boot

| API | 说明 |
|-----|------|
| `TsplPrinterManager.get/send/sendDual/evict/close` | 长连接池 |
| `TsplPrinter.send/sendDual/getLastSendAtMs/close` | 单打印机 |
| `TsplAutoConfiguration` | 自动注册 Manager |

## 预览（test 模块）

| API | 说明 |
|-----|------|
| `TsplPreview.render/toImage/toPng/resolveSize` | 文档 → 画布 / PNG |
| `ImagePreviewOptions` | zoom、圆点、纸色、墨色、边距 |
| `PreviewPaths.dir/file` | 默认写出到 `target/preview` |
