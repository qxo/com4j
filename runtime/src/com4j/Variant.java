package com4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Wraps COM VARIANT data structure.
 *
 * This class allows you to deal with the raw VARIANT type in case you need it,
 * but in general you should bind <tt>VARIANT*</tt> to {@link Object} or
 * {@link Holder<Object>} for more natural Java binding.
 *
 * TODO: more documentation.
 *
 * <h2>Notes</h2>
 * <ol>
 * <li>
 * Calling methods defined on {@link Number} changes the variant
 * type (i.e., similar to a cast in Java) accordingly and returns its value.
 * </ol>
 *
 * <p>
 * Method names that end with '0' are native methods.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Variant extends Number {
    /**
     * The memory image of the VARIANT.
     */
    final ByteBuffer image = ByteBuffer.allocateDirect(16);

    /**
     * VARIANT type.
     *
     * This enum only defines constants that are legal for VARIANTs.
     */
    public static enum Type implements ComEnum {
        VT_EMPTY(0),
        VT_NULL(1),
        VT_I2(2),
        VT_I4(3),
        VT_R4(4),
        VT_R8(5),
        VT_CY(6),
        VT_DATE(7),
        VT_BSTR(8),
        VT_DISPATCH(9),
        VT_ERROR(10),
        VT_BOOL(11),
        VT_VARIANT(12),
        VT_UNKNOWN(13),
        VT_DECIMAL(14),
        VT_RECORD(36),
        VT_I1(16),
        VT_UI1(17),
        VT_UI2(18),
        VT_UI4(19),
        VT_INT(22),
        VT_UINT(23),
//        VT_ARRAY(),
//        VT_BYREF
        ;

        private final int value;

        private Type( int value ) {
            this.value = value;
        }

        public int comEnumValue() {
            return value;
        }
    }

    /**
     * Creates an empty {@link Variant}.
     */
    public Variant() {
        image.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Creates an empty {@link Variant} with the given type.
     */
    public Variant(Type type) {
        this();
        setType(type);
    }

    /**
     * Empties the current contents.
     *
     * <p>
     * Sometimes a {@link Variant} holds things like interface pointers or
     * arrays, which require some clean up actions. Therefore, when you
     * want to reuse an existing {@link Variant} that may hold a value,
     * you should first clear it.
     */
    public void clear() {
        clear0(image);
    }

    /**
     * Makes sure the variant is cleared before GC-ed.
     */
    public void finalize() {
        clear();
    }

    /**
     * Calls <tt>VariantClear</tt> method.
     */
    private static native void clear0( ByteBuffer image );

    /**
     * Sets the type of the variant.
     */
    public void setType( Type t ) {
        image.putLong(0,t.comEnumValue());
    }

    /**
     * Gets the type of the variant.
     */
    public Type getType() {
        return EnumDictionary.get(Type.class).constant((int)image.getLong(0));
    }


    private static native void changeType0( int type, ByteBuffer image );

    /**
     * Changes the variant type to the specified one.
     */
    private void changeType( Type t ) {
        changeType0( t.comEnumValue(), image );
    }

    public int intValue() {
        changeType(Type.VT_I4);
        return image.getInt(8);
    }

    public long longValue() {
        // VARIANT doesn't seem to support 64bit int
        return intValue();
    }

    public float floatValue() {
        changeType(Type.VT_R4);
        return image.getFloat(8);
    }

    public double doubleValue() {
        changeType(Type.VT_R8);
        return image.getDouble(8);
    }

    public <T extends Com4jObject> T object( Class<T> type ) {
        changeType(Type.VT_UNKNOWN);
        int ptr = image.getInt(8);
        if(ptr==0)  return null;
        Native.addRef(ptr);
        return Wrapper.create(type,ptr);
    }

    // TODO: this isn't quite working
    public static final Variant MISSING = new Variant(Type.VT_ERROR);
}
