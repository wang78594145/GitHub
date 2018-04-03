package org.apache.zeppelin.sendMai;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;

public class CSVUtils {
    /**
     * 生成为CVS文件
     *
     * @param exportData 源数据List
     * @param map        csv文件的列表头map
     * @param outPutPath 文件路径
     * @param fileName   文件名称
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static File createCSVFile(List exportData, LinkedHashMap map, String outPutPath,
                                     String fileName) {
        File csvFile = null;
        BufferedWriter csvFileOutputStream = null;
        try {
            File file = new File(outPutPath);
            if (!file.exists()) {
                file.mkdir();
            }
            //定义文件名格式并创建
            csvFile = File.createTempFile(fileName, ".csv", new File(outPutPath));
            System.out.println("csvFile：" + csvFile);
            // UTF-8使正确读取分隔符","
            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    csvFile), "UTF-8"), 1024);
            System.out.println("csvFileOutputStream：" + csvFileOutputStream);
            // 写入文件头部
            for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator.hasNext(); ) {
                java.util.Map.Entry propertyEntry = (java.util.Map.Entry) propertyIterator.next();
                csvFileOutputStream
                        .write("" + (String) propertyEntry.getValue() != null ? (String) propertyEntry
                                .getValue() : "" + "");
                if (propertyIterator.hasNext()) {
                    csvFileOutputStream.write(",");
                }
            }
            csvFileOutputStream.newLine();
            // 写入文件内容
            for (Iterator iterator = exportData.iterator(); iterator.hasNext(); ) {
                Object row = (Object) iterator.next();
                for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator
                        .hasNext(); ) {
                    java.util.Map.Entry propertyEntry = (java.util.Map.Entry) propertyIterator
                            .next();
                    csvFileOutputStream.write((String) BeanUtils.getProperty(row,
                            (String) propertyEntry.getKey()));
                    if (propertyIterator.hasNext()) {
                        csvFileOutputStream.write(",");
                    }
                }
                if (iterator.hasNext()) {
                    csvFileOutputStream.newLine();
                }
            }
            csvFileOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                csvFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return csvFile;
    }

    /**
     * 下载文件
     *
     * @param response
     * @param csvFilePath 文件路径
     * @param fileName    文件名称
     * @throws IOException
     */
    public static void exportFile(HttpServletResponse response, String csvFilePath, String fileName)
            throws IOException {
        response.setContentType("application/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        InputStream in = null;
        try {
            in = new FileInputStream(csvFilePath);
            int len = 0;
            byte[] buffer = new byte[1024];
            response.setCharacterEncoding("UTF-8");
            OutputStream out = response.getOutputStream();
            while ((len = in.read(buffer)) > 0) {
                out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                out.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 删除该目录filePath下的所有文件
     *
     * @param filePath 文件目录路径
     */
    public static void deleteFiles(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    files[i].delete();
                }
            }
        }
    }

    /**
     * 删除单个文件
     *
     * @param filePath 文件目录路径
     * @param fileName 文件名称
     */
    public static void deleteFile(String filePath, String fileName) {
        File file = new File(filePath);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (files[i].getName().equals(fileName)) {
                        files[i].delete();
                        return;
                    }
                }
            }
        }
    }















    public void createCSV() {

                // 表格头
               Object[] head = { "Date", "User", "Action", };
                List<Object> headList = Arrays.asList(head);

                //数据
                List<List<Object>> dataList = new ArrayList<List<Object>>();
                List<Object> rowList = null;
                for (int i = 0; i < 100; i++) {
                        rowList = new ArrayList<Object>();
                        rowList.add("张三" + i);
                        rowList.add("263834194" + i);
                        rowList.add(new Date());
                         dataList.add(rowList);
                     }

                String fileName = "testCSV.csv";//文件名称
                String filePath = "c:/test/"; //文件路径

                File csvFile = null;
                BufferedWriter csvWtriter = null;
                try {
                        csvFile = new File(filePath + fileName);
                        File parent = csvFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                                 parent.mkdirs();
                            }
                        csvFile.createNewFile();

                        // GB2312使正确读取分隔符","
                        csvWtriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "GB2312"), 1024);

                        //文件下载，使用如下代码
             //            response.setContentType("application/csv;charset=gb18030");
             //            response.setHeader("Content-disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
             //            ServletOutputStream out = response.getOutputStream();
             //            csvWtriter = new BufferedWriter(new OutputStreamWriter(out, "GB2312"), 1024);

                         int num = headList.size() / 2;
                         StringBuffer buffer = new StringBuffer();
                         for (int i = 0; i < num; i++) {
                                 buffer.append(" ,");
                             }
                         csvWtriter.write(buffer.toString() + fileName + buffer.toString());
                         csvWtriter.newLine();

                         // 写入文件头部
                         writeRow(headList, csvWtriter);

                         // 写入文件内容
                         for (List<Object> row : dataList) {
                                 writeRow(row, csvWtriter);
                             }
                         csvWtriter.flush();
                     } catch (Exception e) {
                         e.printStackTrace();
                     } finally {
                         try {
                                 csvWtriter.close();
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                     }
             }
    private static void writeRow(List<Object> row, BufferedWriter csvWriter) throws IOException {
                 for (Object data : row) {
                         StringBuffer sb = new StringBuffer();
                         String rowStr = sb.append("\"").append(data).append("\",").toString();
                         csvWriter.write(rowStr);
                     }
                 csvWriter.newLine();
             }

}