package net.onlyjugs.data;

/** Compile-time ABI stub for the vendor record; never included in releases. */
public final class Listing {
    private final int unitPrice;
    public Listing(int unitPrice) { this.unitPrice = unitPrice; }
    public static Listing forSale(int price) { return new Listing(price); }
    public int unitPrice() { return unitPrice; }
}
