package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangSizeInfo implements Serializable {

    private long size;
    private long elementNumber;
    private long collectionNumber;
    private boolean partial;

    public OrtolangSizeInfo() {
        size = 0;
        elementNumber = 0;
        collectionNumber = 0;
        partial = false;
    }

    public OrtolangSizeInfo(long size, long elementNumber, long collectionNumber, boolean partial) {
        this.size = size;
        this.elementNumber = elementNumber;
        this.collectionNumber = collectionNumber;
        this.partial = partial;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void addElementSize(long size) {
        this.size += size;
        this.elementNumber += 1;
    }

    public long getElementNumber() {
        return elementNumber;
    }

    public void setElementNumber(long elementNumber) {
        this.elementNumber = elementNumber;
    }

    public long getCollectionNumber() {
        return collectionNumber;
    }

    public void setCollectionNumber(long collectionNumber) {
        this.collectionNumber = collectionNumber;
    }

    public void incrementCollectionNumber() {
        this.collectionNumber += 1;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }
}
