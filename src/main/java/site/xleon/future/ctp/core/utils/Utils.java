package site.xleon.future.ctp.core.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

import javax.crypto.*;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author leon.xu
 */
public class Utils {
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Object to Map
     * @param obj Object
     * @return map
     */
    public static Map<String, Object> objectToMap(Object obj){
        Map<String, Object> map = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();

            try {
                Object value = field.get(obj);
                if (value == null) {
                    value = "";
                }
                map.put(fieldName, value);
            }catch (Exception e) {
                //
            }
        }
        return map;
    }

    /**
     * 返回对象值
     *
     * @param object object
     * @param name name
     * @return Optional<T>
     */
    public static Optional<Object> getValueByName(Object object, String name) {
        if (object == null) {
            return Optional.empty();
        }

        String methodName = "get" + Utils.StringExtends.firstUppercase(name);
        Object value = null;
        try {
            Method method = object.getClass().getMethod(methodName);
            value = method.invoke(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.ofNullable(value);
    }

    public static Optional<Object> getFieldValueByName(Object object, String fieldName) {
        if (object == null) {
            return Optional.empty();
        }
        Object value = null;
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            value = field.get(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.ofNullable(value);
    }

    /**
     * 分页
     */
    private static final Integer DEFAULT_PAGE_SIZE = 50;
    public static <T> Page<T> page(Integer aPage, Integer aPageSize) {
        Integer page = Optional.ofNullable(aPage).orElse(1);
        Integer pageSize = Optional.ofNullable(aPageSize).orElse(DEFAULT_PAGE_SIZE);
        Page<T> paging = new Page<>((long)page, (long)pageSize);
        paging.setMaxLimit(100L);
        return paging;
    }

    /**
     * 获取北京时间时间戳
     *
     * @param dateTime dateTime
     * @return long
     */
    public static long getTime(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }


    /**
     * Enum Extends
     */
    public static class EnumExtends {
        private EnumExtends() {
        }

        /**
         * enumOf 通过value构造enum, value可以为null, 不能非法
         */
        public static <T> T enumOf(Class<T> enumClass, Object value) throws Exception {
            if (value == null) {
                return null;
            }
            Method valuesMethod = enumClass.getDeclaredMethod("values");
            T[] enums = (T[]) valuesMethod.invoke(enumClass);

            for (T item : enums) {
                Field field = item.getClass().getDeclaredField("value");
                field.setAccessible(true);
                Object enumValue = field.get(item);
                if (enumValue.equals(value)) {
                    return item;
                }
            }
            throw new Exception("create enum '" + enumClass.getName() + "' fail by invalid value: " + value);
        }
    }

    /**
     * Long extends
     */
    public static class LongExtends {
        private LongExtends() {
        }

        /**
         * timestamp to date string
         *
         * @param timestamp 时间戳
         * @return date string
         */
        public static String getDateString(Long timestamp, String pattern, boolean isGmt) {
            if (timestamp ==  null) {
                return null;
            }

//            YdAssert.of(pattern)
//                    .notEmpty()
//                    .failure(value -> value = "yyyy-MM-dd HH:mm:ss");
            long ms = timestamp;
            if (isGmt) {
                ms = timestamp - TimeZone.getDefault().getRawOffset();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
            return dateFormat.format(ms);
        }

        public static String getDateString(Long timestamp) {
            return LongExtends.getDateString(timestamp, "yyyy-MM-dd HH:mm:ss", false);
        }


        /**
         * bytes 转 kb mb gb
         *
         * @param size size
         * @return 单位
         */
        public static String getPrintSize(Long size, String pattern) {
            String aPattern = Optional.ofNullable(pattern).orElse("###.00");
            StringBuilder bytes = new StringBuilder();
            DecimalFormat format = new DecimalFormat(aPattern);
            if (size >= 1024L * 1024L * 1024L * 1024L) {
                double i = (size / (1024.0 * 1024.0 * 1024.0 * 1024.0));
                bytes.append(format.format(i)).append("TB");
            } else if (size >= 1024L * 1024L * 1024L) {
                double i = (size / (1024.0 * 1024.0 * 1024.0));
                bytes.append(format.format(i)).append("GB");
            } else if (size >= 1024L * 1024L) {
                double i = (size / (1024.0 * 1024.0));
                bytes.append(format.format(i)).append("MB");
            } else if (size >= 1024L) {
                double i = (size / (1024.0));
                bytes.append(format.format(i)).append("KB");
            } else {
                bytes.append("0B");
            }
            return bytes.toString();
        }
    }

    /**
     * String extends
     */
    public static class StringExtends {
        private StringExtends() {
        }

        private static final String SLAT = "20e26cafd6a7";

        /**
         * String to md5
         *
         * @param string input string
         * @return md5
         */
        public static String getMd5(String string, String slat) {
            String base = string;
            if (slat != null) {
                base = String.format("%s%s", string, slat);
            }
            return DigestUtils.md5DigestAsHex(base.getBytes()).toLowerCase();
        }

        /**
         * 首字母大写
         *
         * @param string input string
         * @return new  string
         */
        public static String firstUppercase(String string) {
            if (string == null || "".equals(string)) {
                return string;
            }
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }

        /**
         * 首字母小写
         *
         * @param string string
         * @return new string
         */
        public static String firstLowercase(String string) {
            if (string == null || "".equals(string)) {
                return string;
            }
            return string.substring(0, 1).toLowerCase() + string.substring(1);
        }

        /**
         * 字符串压缩
         * @param unzipString unzip string
         * @return base64 string
         */
        public static String zipString(String unzipString) {
            //使用指定的压缩级别创建一个新的压缩器。
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            //设置压缩输入数据。
            deflater.setInput(unzipString.getBytes());
            //当被调用时，表示压缩应该以输入缓冲区的当前内容结束。
            deflater.finish();

            final byte[] bytes = new byte[4096];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4096);

            while (!deflater.finished()) {
                //压缩输入数据并用压缩数据填充指定的缓冲区。
                int length = deflater.deflate(bytes);
                outputStream.write(bytes, 0, length);
            }
            //关闭压缩器并丢弃任何未处理的输入。
            deflater.end();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    /**
     * File extends
     */
    public static class FileExtends {
        private FileExtends() {}

        /**
         * 读取文件
         * @param fileUrl 文件url
         * @return 文件内容 string
         */
        public static String getString(String fileUrl){
            StringBuilder sb = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileUrl))){
                String str;
                while((str = bufferedReader.readLine()) != null) {
                    sb.append(str);
                    sb.append("\r\n");
                }
                int len = sb.length();
                if (len > 0) {
                    sb.delete(len-2, len);
                }
            }catch (Exception exception) {
                return null;
            }

            return sb.toString();
        }

        /**
         * 创建目录，如果没有
         * @param path 目录
         * @return is success
         */
        public static boolean initPath(String path) {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                return true;
            }
            return file.mkdirs();
        }

        public static void compress(String sourceUrl, String destUrl ) throws IOException {
            File destFile = new File(destUrl);
            if (!destFile.exists()) {
                Files.createFile(destFile.toPath());
            }

            try (
                FileOutputStream output = new FileOutputStream(destFile);
                ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(output, 8096))
            ){
                compress(sourceUrl, zipOutput, null);
            }
        }

        /**
         * 压缩文件
         *
         * @param inputFileUrl input file url
         * @throws IOException exception
         */
        private static void compress(String inputFileUrl, ZipOutputStream zipOutput, String basePath) throws IOException {
            // create zip file
            File inputFile = new File(inputFileUrl);
            if (!inputFile.exists()) {
                return;
            }

            if (basePath == null) {
                basePath = inputFile.getAbsolutePath();
            }

            if (inputFile.isDirectory() && inputFile.listFiles() != null) {
                for (File file: Objects.requireNonNull(inputFile.listFiles())) {
                    compress(file.getAbsolutePath(), zipOutput, basePath);
                }
            } else {
                try (
                        FileInputStream inputStream = new FileInputStream(inputFileUrl);
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 8096)
                ) {
                    String filePath = inputFile.getAbsolutePath();
                    if (!filePath.equals(basePath)) {
                        filePath = filePath.substring(basePath.length() + 1);
                    }else{
                        filePath = inputFile.getName();
                    }
                    zipOutput.putNextEntry(new ZipEntry(filePath));
                    int len;
                    byte[] data =  new byte[8096];
                    while ((len = bufferedInputStream.read(data)) != -1) {
                        zipOutput.write(data, 0, len);
                    }
                }
            }
        }

        /**
         * 文件解压
         * @param inputFileUrl input file url
         * @param outputFileUrl output file url
         * @return 文件
         * @throws IOException exception
         */
        public static List<File> uncompress(String inputFileUrl, String outputFileUrl) throws IOException {
            File inputFile = new File(inputFileUrl);
            ArrayList<File> lists = new ArrayList<>();
            try (ZipInputStream inputStream = new ZipInputStream(new FileInputStream(inputFile))){
                ZipEntry entry = null;
                File file = null;
                while ((entry = inputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        file = new File(outputFileUrl, entry.getName());
                        lists.add(file);
                        initPath(file.getParent());
                        OutputStream outputStream = new FileOutputStream(file);
                        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)){
                            int len = -1;
                            byte[] buff = new byte[1024*1024];
                            while((len = inputStream.read(buff)) != -1) {
                                bufferedOutputStream.write(buff, 0, len);
                            }
                        }
                    }
                }
            }

            return lists;
        }

        public static String md5Sum(String fileUrl, String salt){
            String fileString = Utils.FileExtends.getString(fileUrl);
            if (fileString == null) {
                return null;
            }

            fileString += "\n";
            return Utils.StringExtends.getMd5(
                    fileString, salt
            );
        }

        public static String getFileHeader(File file) {
            try(FileInputStream inputStream = new FileInputStream(file)) {
                byte[] bytes = new byte[4];
                inputStream.read(bytes, 0, bytes.length);
                return DatatypeConverter.printHexBinary(bytes);
            }catch (Exception exception) {
                return null;
            }
        }

        private static final String ELF = "7F454C46";
        public static boolean isElf(File file) {
            String fileHeader = getFileHeader(file);
            if (ELF.equalsIgnoreCase(fileHeader)) {
                return true;
            }
            return false;
        }
    }

    public static class CheckIdCard{
        private CheckIdCard() {}

        private static final String BIRTH_DATE_FORMAT = "yyyyMMdd"; // 身份证号码中的出生日期的格式

        private static final Date MINIMAL_BIRTH_DATE = new Date(-2209017600000L); // 身份证的最小出生日期,1900年1月1日

        private static final int NEW_CARD_NUMBER_LENGTH = 18;

        private static final int OLD_CARD_NUMBER_LENGTH = 15;

        private static final char[] VERIFY_CODE = { '1', '0', 'X', '9', '8', '7',
                '6', '5', '4', '3', '2' }; // 18位身份证中最后一位校验码

        private static final int[] VERIFY_CODE_WEIGHT = { 7, 9, 10, 5, 8, 4, 2, 1,
                6, 3, 7, 9, 10, 5, 8, 4, 2 }; // 18位身份证中，各个数字的生成校验码时的权值


        /**
         * 如果是15位身份证号码，则自动转换为18位
         * 可以为null
         *
         * @param cardNumber 身份证号
         * @return bool success or false
         */
        public static boolean check(String cardNumber){
            if (null == cardNumber){
                return true;
            }

            cardNumber = cardNumber.trim();
            if (OLD_CARD_NUMBER_LENGTH == cardNumber.length()){
                cardNumber = convertToNewCardNumber(cardNumber);
            }
            return validate(cardNumber);
        }

        public static boolean validate(String cardNumber){
            boolean result = true;
            result = result && (null != cardNumber); // 身份证号不能为空
            result = result && NEW_CARD_NUMBER_LENGTH == cardNumber.length(); // 身份证号长度是18(新证)
            // 身份证号的前17位必须是阿拉伯数字
            for (int i = 0; result && i < NEW_CARD_NUMBER_LENGTH - 1; i++){
                char ch = cardNumber.charAt(i);
                result = result && ch >= '0' && ch <= '9';
            }
            // 身份证号的第18位校验正确
            result = result
                    && (calculateVerifyCode(cardNumber) == cardNumber
                    .charAt(NEW_CARD_NUMBER_LENGTH - 1));
            // 出生日期不能晚于当前时间，并且不能早于1900年
            try{
                Date birthDate = new SimpleDateFormat(BIRTH_DATE_FORMAT)
                        .parse(getBirthDayPart(cardNumber));
                result = result && null != birthDate;
                result = result && birthDate.before(new Date());
                result = result && birthDate.after(MINIMAL_BIRTH_DATE);
                /**
                 * 出生日期中的年、月、日必须正确,比如月份范围是[1,12],
                 * 日期范围是[1,31]，还需要校验闰年、大月、小月的情况时，
                 * 月份和日期相符合
                 */
                String birthdayPart = getBirthDayPart(cardNumber);
                String realBirthdayPart = new SimpleDateFormat(BIRTH_DATE_FORMAT)
                        .format(birthDate);
                result = result && (birthdayPart.equals(realBirthdayPart));
            }catch(Exception e){
                result = false;
            }
            return result;
        }

        private static String getBirthDayPart(String cardNumber){
            return cardNumber.substring(6, 14);
        }

        /**
         * 校验码（第十八位数）：
         *
         * 十七位数字本体码加权求和公式 S = Sum(Ai * Wi), i = 0...16 ，先对前17位数字的权求和；
         * Ai:表示第i位置上的身份证号码数字值 Wi:表示第i位置上的加权因子 Wi: 7 9 10 5 8 4 2 1 6 3 7 9 10 5 8 4
         * 2; 计算模 Y = mod(S, 11)< 通过模得到对应的校验码 Y: 0 1 2 3 4 5 6 7 8 9 10 校验码: 1 0 X 9
         * 8 7 6 5 4 3 2
         *
         * @param cardNumber card number
         * @return 校验码
         */
        private static char calculateVerifyCode(CharSequence cardNumber){
            int sum = 0;
            for (int i = 0; i < NEW_CARD_NUMBER_LENGTH - 1; i++){
                char ch = cardNumber.charAt(i);
                sum += ((int) (ch - '0')) * VERIFY_CODE_WEIGHT[i];
            }
            return VERIFY_CODE[sum % 11];
        }

        /**
         * 把15位身份证号码转换到18位身份证号码<br>
         * 15位身份证号码与18位身份证号码的区别为：<br>
         * 1、15位身份证号码中，"出生年份"字段是2位，转换时需要补入"19"，表示20世纪<br>
         * 2、15位身份证无最后一位校验码。18位身份证中，校验码根据根据前17位生成
         *
         * @param oldCardNumber old card number
         * @return card number
         */
        private static String convertToNewCardNumber(String oldCardNumber){
            StringBuilder buf = new StringBuilder(NEW_CARD_NUMBER_LENGTH);
            buf.append(oldCardNumber.substring(0, 6));
            buf.append("19");
            buf.append(oldCardNumber.substring(6));
            buf.append(CheckIdCard.calculateVerifyCode(buf));
            return buf.toString();
        }
    }

    /**
     * SHA0256
     */
    public static class SHA256 {
        private SHA256() {};

        public static String getSHA256(String string) throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(string.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
        }
    }

    /**
     * RSA 加密/解密
     */
    public static class RSAExtends {
        private RSAExtends() {}

        private static final Logger logger = LoggerFactory.getLogger(RSAExtends.class);
        private static final Base64.Decoder DECODER = Base64.getDecoder();
        private static final String KEY_ALGORITHM = "RSA/ECB/PKCS1Padding";
        private static final String KEY_AES = "AES/ECB/PKCS5Padding";

        /**
         * 对文件加密
         * @param publicKeyUrl 公钥保存路径
         * @param inputFileUrl 加密文件路径
         * @param outputFileUrl 加密后文件路径
         */
        public static void encryptFile(String publicKeyUrl, String inputFileUrl, String outputFileUrl) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException {
            String publicKeyString = Utils.FileExtends.getString(publicKeyUrl);
            // 移除所有换行符
            publicKeyString = publicKeyString.replace("\r", "");
            publicKeyString = publicKeyString.replace("\n", "");
            //
            byte[] keyBytes = DECODER.decode(publicKeyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Cipher
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.WRAP_MODE, publicKey);

            //
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom random = new SecureRandom();
            keyGenerator.init(random);
            SecretKey key = keyGenerator.generateKey();
            byte[] wrappedKey = cipher.wrap(key);

            // out
            try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFileUrl))){
                outputStream.writeInt(wrappedKey.length);
                outputStream.write(wrappedKey);

                // input
                try(InputStream inputStream = Files.newInputStream(Paths.get(inputFileUrl))) {
                    cipher = Cipher.getInstance(KEY_AES);
                    cipher.init(Cipher.ENCRYPT_MODE, key);
                    process(inputStream, outputStream, cipher);
                } catch (ShortBufferException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * 解密
         * @param privateKeyUrl 私钥文件路径
         * @param inputFileUrl 待解密文件路径
         * @param outputFileUrl 解密后的文件路径
         */
        public static void decrypt(String privateKeyUrl, String inputFileUrl, String outputFileUrl) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
            // private key
            String privateKeyString = Utils.FileExtends.getString(privateKeyUrl);
            // 移除所有换行符
            privateKeyString = privateKeyString.replace("\r", "");
            privateKeyString = privateKeyString.replace("\n", "");
            byte[] keyBytes = DECODER.decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            //
            try (DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFileUrl))) {
                int length = inputStream.readInt();
                byte[] wrappedKey = new byte[length];
                inputStream.read(wrappedKey, 0, length);

                // cipher
                Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
                cipher.init(Cipher.UNWRAP_MODE, privateKey);
                Key key = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
                // output stream
                try (OutputStream outputStream = new FileOutputStream(outputFileUrl)) {
                    cipher = Cipher.getInstance(KEY_AES);
                    cipher.init(Cipher.DECRYPT_MODE, key);

                    process(inputStream, outputStream, cipher);
                }
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (ShortBufferException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }

        private static void process(InputStream inputStream, OutputStream outputStream, Cipher cipher) throws IOException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
            int blockSize = cipher.getBlockSize();
            int outputSize = cipher.getOutputSize(blockSize);
            byte[] inBytes = new byte[blockSize];
            byte[] outBytes = new byte[outputSize];

            int inLength = 0;
            boolean next = true;
            while (next) {
                inLength = inputStream.read(inBytes);
                if (inLength == blockSize) {
                    int outLength = cipher.update(inBytes, 0, blockSize, outBytes);
                    outputStream.write(outBytes, 0, outLength);
                }else{
                    next = false;
                }
            }

            if (inLength >0) {
                outBytes = cipher.doFinal(inBytes, 0, inLength);
            }else {
                outBytes = cipher.doFinal();
            }
            outputStream.write(outBytes);
        }

        /**
         * 生成　RSA
         * @throws NoSuchAlgorithmException
         */
        private static void generateKey() throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            Base64.Encoder encoder = Base64.getEncoder();
            logger.info("public key: {}", encoder.encodeToString(publicKey.getEncoded()));
            logger.info("private key: {}", encoder.encodeToString(privateKey.getEncoded()));
        }
    }

    public static String getIp(HttpServletRequest request) {
        // nginx
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.length() == 0  || "unknown".equalsIgnoreCase(ip)) {
            // Squid
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0  || "unknown".equalsIgnoreCase(ip)) {
            // apache
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0  || "unknown".equalsIgnoreCase(ip)) {
            // weblogic
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0  || "unknown".equalsIgnoreCase(ip)) {
            // others
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0  || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0  || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * 中位数
     * @param array
     * @return
     */
    public static Double getMedian(List<Double> array) {
        if (array == null || array.isEmpty()) {
            return 0.0;
        }
        double median;
        array.sort(Comparator.naturalOrder());
        // 中位
        if (array.size() % 2 == 0) {
            // 偶
            int index = array.size() / 2;
            median = (array.get(index) + array.get(index - 1)) / 2;
        }else {
            // 奇
            int index = (array.size() - 1) / 2;
            median = array.get(index);
        }

        return median;
    }

    public static Long getLongMedian(List<Long> array) {
        if (array == null || array.isEmpty()) {
            return 0L;
        }
        long median;
        array.sort(Comparator.naturalOrder());
        // 中位
        if (array.size() % 2 == 0) {
            // 偶
            int index = array.size() / 2;
            median = (array.get(index) + array.get(index - 1)) / 2;
        }else {
            // 奇
            int index = (array.size() - 1) / 2;
            median = array.get(index);
        }

        return median;
    }
}
