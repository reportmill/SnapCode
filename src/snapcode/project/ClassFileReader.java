/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads a class file.
 *
 * @author Tanmay K. Mohapatra
 * @version 1.03, 28th Sep, 2003
 */
class ClassFileReader {

    // Version numbers
    protected int magicNumber;
    protected int minorVersion;
    protected int majorVersion;

    // The constant pool
    private ConstantPool constantPool = new ConstantPool();

    // The class names
    private ClassNames classNames = new ClassNames();

    /**
     * reads the class file into data structures.
     */
    public void read(DataInputStream dis) throws IOException
    {
        // Read Version Numbers
        magicNumber = dis.readInt();
        minorVersion = dis.readUnsignedShort();
        majorVersion = dis.readUnsignedShort();

        // Read constants table
        constantPool.read(dis);

        // Read access flags
        dis.readUnsignedShort(); // Read iAccessFlags
        // new AccessFlags().read(dis);

        // Read ClassNames
        classNames.read(dis, constantPool);
    }

    /**
     * Returns the number of constants.
     */
    public int getConstantCount()  { return constantPool.getPoolInfoCount(); }

    /**
     * Returns the individual constant at given index.
     */
    public Constant getConstant(int anIndex)  { return constantPool.getPoolInfo(anIndex); }

    /**
     * ConstantPool.
     */
    public static class ConstantPool {

        // as per class file entry
        private int constantCount;

        // number of actual pool infos
        private int iNumPoolInfos;

        // collection of pool infos
        private List<Constant> _constants;

        /**
         * Read constants.
         */
        void read(DataInputStream dis) throws IOException
        {
            // const pool index 0 is not present in the class file and is for internal use of JVMs
            constantCount = dis.readUnsignedShort();
            iNumPoolInfos = constantCount - 1;
            _constants = new ArrayList<>(iNumPoolInfos);
            for (int i = 0; i < iNumPoolInfos; i++) {
                Constant newInfo = new Constant();
                newInfo.read(dis);
                _constants.add(newInfo);
                if (newInfo.isDoubleSizeConst()) {
                    _constants.add(newInfo);
                    i++;
                }// check how indexOf works
            }

            // Resolve references
            constantCount = (iNumPoolInfos = _constants.size()) + 1;
            for (int i = 0; i < iNumPoolInfos; i++) {
                Constant newInfo = (Constant) _constants.get(i);
                newInfo.resolveReferences(this);
                if (newInfo.isDoubleSizeConst()) i++;
            }
        }

        public String toString()
        {
            constantCount = (iNumPoolInfos = _constants.size()) + 1;
            String sRetStr = "Constant pool. Count: " + constantCount;
            return sRetStr;
        }

        // The index is the value as referred to in the class file. The first valid index is 1.
        public Constant getPoolInfo(int iIndex)
        {
            return _constants.get(iIndex - 1);
        }

        public int getPoolInfoCount()
        {
            constantCount = (iNumPoolInfos = _constants.size()) + 1;
            return iNumPoolInfos;
        }
    }

    /**
     * A class to represent a constant from the Class file Constant Pool Table.
     */
    public static class Constant {

        // Constant pool types
        public static final int CONSTANT_Class = 7;
        public static final int CONSTANT_Fieldref = 9;
        public static final int CONSTANT_Methodref = 10;
        public static final int CONSTANT_InterfaceMethodref = 11;
        public static final int CONSTANT_String = 8;
        public static final int CONSTANT_Integer = 3;
        public static final int CONSTANT_Float = 4;
        public static final int CONSTANT_Long = 5;
        public static final int CONSTANT_Double = 6;
        public static final int CONSTANT_NameAndType = 12;
        public static final int CONSTANT_Utf8 = 1;
        public static final int CONSTANT_MethodHandle = 15;
        public static final int CONSTANT_MethodType = 16;
        public static final int CONSTANT_InvokeDynamic = 18;

        /**
         * Tag denotes the type of pool entry. It will be one of CONSTANT_<...> types
         */
        public int iTag;
        public int iNameIndex;
        public int iClassIndex;
        public int iNameAndTypeIndex;
        public int iStringIndex;
        public int iIntValue;
        public float fFloatVal;
        public long lLongVal;
        public int iDescriptorIndex;
        public double dDoubleVal;
        public String sUTFStr;
        public int iReferenceKind;
        public int iReferenceIndex;
        public int iBootstrapMethodAttrIndex;

        ConstantPool constPool;
        public Constant refUTF8, refExtraUTF8, refClass, refNameAndType;


        void read(DataInputStream dis) throws IOException
        {
            iTag = dis.readByte();

            switch (iTag) {
                case CONSTANT_Class:
                    iNameIndex = dis.readUnsignedShort();
                    break; // points to a UTF8
                case CONSTANT_String:
                    iStringIndex = dis.readUnsignedShort();
                    break; // points to a UTF8
                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    iClassIndex = dis.readUnsignedShort(); // points to a Class
                    iNameAndTypeIndex = dis.readUnsignedShort(); // points to a NameAndType
                    break;
                case CONSTANT_Integer: iIntValue = dis.readInt(); break;
                case CONSTANT_Float: fFloatVal = dis.readFloat(); break;
                case CONSTANT_NameAndType:
                    iNameIndex = dis.readUnsignedShort(); // points to UTF8
                    iDescriptorIndex = dis.readUnsignedShort(); // points to UTF8
                    break;
                case CONSTANT_Long: lLongVal = dis.readLong(); break;
                case CONSTANT_Double: dDoubleVal = dis.readDouble(); break;
                case CONSTANT_Utf8: sUTFStr = dis.readUTF(); break;
                case CONSTANT_MethodHandle:
                    iReferenceKind = dis.readByte();
                    iReferenceIndex = dis.readUnsignedShort();
                    break;
                case CONSTANT_MethodType: iDescriptorIndex = dis.readUnsignedShort(); break;
                case CONSTANT_InvokeDynamic:
                    iBootstrapMethodAttrIndex = dis.readUnsignedShort();
                    iNameAndTypeIndex = dis.readUnsignedShort();
                    break;
                default: System.out.println("ClassFileReader: Unknown constant pool type: " + iTag); break;
            }
        }

        void resolveReferences(ConstantPool constPool)
        {
            this.constPool = constPool;
            switch (iTag) {
                case CONSTANT_Class:
                    refUTF8 = constPool.getPoolInfo(iNameIndex);
                    break;
                case CONSTANT_String:
                    refUTF8 = constPool.getPoolInfo(iStringIndex);
                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    refClass = constPool.getPoolInfo(iClassIndex);
                    refNameAndType = constPool.getPoolInfo(iNameAndTypeIndex);
                    break;
                case CONSTANT_NameAndType:
                    refUTF8 = constPool.getPoolInfo(iNameIndex);
                    refExtraUTF8 = constPool.getPoolInfo(iDescriptorIndex);
                    break;
                case CONSTANT_MethodType:
                    refUTF8 = constPool.getPoolInfo(iDescriptorIndex);
                    break;
            }
        }

        /**
         * Returns whether constant is Class type.
         */
        public boolean isClass()  { return iTag == CONSTANT_Class; }

        /**
         * Returns whether constant is Field reference type.
         */
        public boolean isField()  { return iTag == CONSTANT_Fieldref; }

        /**
         * Returns whether constant is constructor reference type.
         */
        public boolean isConstructor()  { return iTag == CONSTANT_Methodref && getMemberName().equals("<init>"); }

        /**
         * Returns whether constant is Method reference type.
         */
        public boolean isMethod()  { return iTag == CONSTANT_Methodref || iTag == CONSTANT_InterfaceMethodref; }

        /**
         * Returns the class name.
         */
        public String getClassName()
        {
            String className = refUTF8.sUTFStr.replace('/', '.');
            //if(cname.startsWith("[")) cname = getType(cname).replace("[]", "");
            return className;
        }

        /**
         * Returns the method declaring class name.
         */
        public String getMemberName()  { return refNameAndType.refUTF8.sUTFStr; }

        /**
         * Returns the declaring class name of method/field.
         */
        public String getDeclClassName()  { return refClass.refUTF8.sUTFStr.replace('/', '.'); }

        /**
         * Returns the type/return-type of method/field.
         */
        public String getType()
        {
            String aStr = refNameAndType.refExtraUTF8.sUTFStr.replace('/', '.');
            if (isMethod()) {
                int i = aStr.indexOf(')');
                aStr = aStr.substring(i + 1);
            }
            return getType(aStr);
        }

        /**
         * Returns the type/return-type of method/field.
         */
        public String getType(String aStr)
        {
            String type = null;
            int acount = 0;
            for (int i = 0, iMax = aStr.length(); i < iMax; i++) {
                char c = aStr.charAt(i);
                if (c == ')') break;
                switch (c) {
                    case 'B': type = "byte"; break;
                    case 'C': type = "char"; break;
                    case 'D': type = "double"; break;
                    case 'F': type = "float"; break;
                    case 'I': type = "int"; break;
                    case 'J': type = "long"; break;
                    case 'S': type = "short"; break;
                    case 'Z': type = "boolean"; break;
                    case 'V': type = "void"; break;
                    case 'L':
                        int end = aStr.indexOf(';', i + 1);
                        type = aStr.substring(i + 1, end);
                        i = end;
                        break;
                    case '[':
                        acount++;
                        continue;
                }
                while (acount > 0) {
                    type += "[]";
                    acount--;
                }
            }
            return type;
        }

        /**
         * Returns the method types.
         */
        public String[] getParameterTypes()
        {
            String aStr = refNameAndType.refExtraUTF8.sUTFStr.replace('/', '.');
            String[] types = new String[8];
            int tcount = 0;
            int acount = 0;
            for (int i = 0, iMax = aStr.length(); i < iMax; i++) {
                char c = aStr.charAt(i);
                if (c == ')') break;
                switch (c) {
                    case 'B': types[tcount++] = "byte"; break;
                    case 'C': types[tcount++] = "char"; break;
                    case 'D': types[tcount++] = "double"; break;
                    case 'F': types[tcount++] = "float"; break;
                    case 'I': types[tcount++] = "int"; break;
                    case 'J': types[tcount++] = "long"; break;
                    case 'S': types[tcount++] = "short"; break;
                    case 'Z': types[tcount++] = "boolean"; break;
                    case 'V': types[tcount++] = "void"; break;
                    case 'L':
                        int end = aStr.indexOf(';', i + 1);
                        types[tcount++] = aStr.substring(i + 1, end);
                        i = end;
                        break;
                    case '[':
                        acount++;
                        continue;
                }
                while (acount > 0) {
                    types[tcount - 1] += "[]";
                    acount--;
                }
                if (tcount == types.length)
                    types = Arrays.copyOf(types, types.length * 2);
            }
            return Arrays.copyOf(types, tcount);
        }

        /**
         * Returns the tag name.
         */
        public String getTagName()
        {
            switch (iTag) {
                case CONSTANT_Class: return "CLASS";
                case CONSTANT_Fieldref: return "FIELDREF";
                case CONSTANT_Methodref: return "METHODREF";
                case CONSTANT_InterfaceMethodref: return "INTERFACEMETHODREF";
                case CONSTANT_String: return "STRING";
                case CONSTANT_Integer: return "INTEGER";
                case CONSTANT_Float: return "FLOAT";
                case CONSTANT_Long: return "LONG";
                case CONSTANT_Double: return "DOUBLE";
                case CONSTANT_NameAndType: return "NAMEANDTYPE";
                case CONSTANT_Utf8: return "UTF8";
                default: return "Unknown";
            }
        }

        public String toString()
        {
            String sDesc = "";
            switch (iTag) {
                case CONSTANT_Class:
                    sDesc = "name=" + refUTF8.sUTFStr;
                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    sDesc = "class=" + refClass.refUTF8.sUTFStr +
                            ", name=" + refNameAndType.refUTF8.sUTFStr + ", type=" + refNameAndType.refExtraUTF8.sUTFStr;
                    break;
                case CONSTANT_String: sDesc = "string=" + refUTF8.sUTFStr; break;
                case CONSTANT_Integer: sDesc = "int_value=" + iIntValue; break;
                case CONSTANT_Float: sDesc = "float_value=" + fFloatVal; break;
                case CONSTANT_Long: sDesc = "long_value=" + lLongVal; break;
                case CONSTANT_Double: sDesc = "double_value=" + dDoubleVal; break;
                case CONSTANT_NameAndType: sDesc = "name=" + refUTF8.sUTFStr + ", descriptor=" + refExtraUTF8.sUTFStr; break;
                case CONSTANT_Utf8: sDesc = "string=" + sUTFStr; break;
            }

            return getTagName() + ": " + sDesc;
        }

        public boolean isDoubleSizeConst()  { return iTag == CONSTANT_Long || iTag == CONSTANT_Double; }
    }

    /**
     * Class to handle class names.
     */
    public static class ClassNames {

        int iThisClass;
        int iSuperClass;
        public Constant cpThisClass;  //CONSTANT_Class
        public Constant cpSuperClass; //CONSTANT_Class

        void read(DataInputStream dis, ConstantPool constPool) throws IOException
        {
            iThisClass = dis.readUnsignedShort();
            iSuperClass = dis.readUnsignedShort();
            cpThisClass = constPool.getPoolInfo(iThisClass);
            cpSuperClass = constPool.getPoolInfo(iSuperClass);
        }

        public String getThisClassName()
        {
            return convertClassStrToStr(cpThisClass.refUTF8.sUTFStr);
        }

        public String getSuperClassName()
        {
            return convertClassStrToStr(cpSuperClass.refUTF8.sUTFStr);
        }

        public String toString()
        {
            String sRetStr = "This class: " + cpThisClass + ", ";
            sRetStr += ("Super class: " + cpSuperClass);
            return sRetStr;
        }
    }

    public static String convertClassStrToStr(String sInStr)
    {
        return sInStr.replace('/', '.');
    }
}