package tech.muyi.tspl.builder;

/**
 * {@code QRCODE} 指令的可选参数。
 */
public final class QrOptions {

    private final String ecc;
    private final int cellWidth;
    private final String mode;
    private final int rotation;
    private final String model;

    private QrOptions(String ecc, int cellWidth, String mode, int rotation, String model) {
        this.ecc = ecc;
        this.cellWidth = cellWidth;
        this.mode = mode;
        this.rotation = rotation;
        this.model = model;
    }

    public static QrOptions of(String ecc, int cellWidth) {
        return new QrOptions(ecc, cellWidth, "A", 0, "M2");
    }

    public static QrOptions defaults() {
        return of("L", 6);
    }

    public QrOptions mode(String mode) {
        return new QrOptions(ecc, cellWidth, mode, rotation, model);
    }

    public QrOptions rotation(int rotation) {
        return new QrOptions(ecc, cellWidth, mode, rotation, model);
    }

    public QrOptions model(String model) {
        return new QrOptions(ecc, cellWidth, mode, rotation, model);
    }

    public String ecc() {
        return ecc;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public String mode() {
        return mode;
    }

    public int rotation() {
        return rotation;
    }

    public String model() {
        return model;
    }
}
