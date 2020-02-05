/* Мой первый проект на JAVA (Ну, надо же с чего-то начинать). 
  Реализация алгоритма слиянием отсортированных файлов большого размера
  Идея алгоритма: последовательность файлов с вводными данными разбиваем на блоки,
  размер блока определяется исходя их наличия свободной памяти (вычислияем объем свободной памяти,
  размер каждого блока равен половине свободной памяти). Каждый блок сохраняем в отдельный временный файл.
  Затем последовательно загружаем оновременно два файла в массив и осуществляем процедуру слияния
  исходя из того, что данные в каждом блоке отсортированны (входное условие задачи). 
  После слияния двух блоков (файлов) в массиве перезаписываем часть массива во временный файл
  и очищаем ту часть массива, которую занимал перезаписанный блок. В памяти у нас осталась
  часть данных двух блоков (файлов) с мимнимальными значениями (в случаи сортировки по возрастанию).
  Таким образом проходим первый цикл после, которого получаем все минимальные значения и сохраняем результат
  в итоговый файл. Затем делаем следующий проход и получаем следующий блок данных. 
  

  В текущей версии программы добавлена сортировка по возрастанию.

    
  Текстовые входные файлы подготавливаются в кодировке Windows-1251(для кириллицы).  
 */
package mergefilesjava;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;
/*
 * @author koshelyuk_sergey@mail.ru
 */
public class MergeFilesJava {
    
    /*вывод на консоль справки пользователя*/
    private static void displayHelp() {
        System.out.println("Програма MergeFilesJava.exe реализует алгоритм слияния предварительно отсортированных файлов");
        System.out.println("Параметры запуска: ");
        System.out.println("[тип входных данных (обязательный параметр)]:[порядок сортировки (не обязательный параметр)]:[имя выходного файла (обязательный параметр)]:[имена входных файлов через пробел(не менее одного)]");
        System.out.println("Аргументы запуска: ");
        System.out.println("-s: тип входных данных строка");
        System.out.println("-i: тип входных данных целое число");
        System.out.println("-a: сортировка по возрастанию (по умалчанию)");
        System.out.println("-d: сортировка по убыванию");                
        System.out.println("-h: вызов справки");
        System.out.println("Примеры запуска: ");
        System.out.println("mergefilesjava.exe -i -a out.txt in.txt (для целых чисел по возрастанию)");
        System.out.println("mergefilesjava.exe -s out.txt in1.txt in2.txt in3.txt (для строк по возрастанию)");
        System.out.println("mergefilesjava.exe -d -s out.txt in1.txt in2.txt (для строк по убыванию)");
    }
    
    public static void main(String[] args) throws IOException { 
        String outFile = null;
        ArrayList<String> inFile = new ArrayList<String>();        
        boolean sort = true;        
        boolean type = true;//1-строка, 0-число
        
        /*установка кодироки для коректного отображения кириллицы*/ 
        setEncoding();                                   
        if (args.length == 0) { displayHelp();  return;}
        
        for (int param = 0; param < args.length; param++) {            
            if (args[param].equals("-h")) {
                displayHelp(); 
                return;           
            } else if (args[param].equals("-d")) {
                sort = false;
            } else if (args[param].equals("-a")) {
                sort = true;
            } else if (args[param].equals("-i")) {
                 type = false; 
            } else if (args[param].equals("-s")) {
                 type = true; 
            } else {
                   if (outFile == null) outFile = args[param];
                   else inFile.add(args[param]);
            }                  
        }                                         
        /*проверяем ввод пользователем имени выходного файла*/
        if (outFile == null) {
            System.out.println("Ошибка! Необходимо ввести имя выходного файла!");
            displayHelp();
            return;   
        }              
        /*проверяем ввод пользователем входных данных (файлов)*/
        if (inFile.size() == 0) {
           System.out.println("Ошибка! Необходимо ввести имя входного файла!");
           displayHelp();
           return;
        }
        /*вызываем процедуру слияния файлов*/
        try { 
            mergeFile(inFile,outFile,sort, type);
            } catch (FileNotFoundException err) {  
                 System.out.println("Ошибка файл не найден! " + "mergefilesjava.mergeFile() " + err.toString());
            } catch (IOException err) {
                 System.out.println("Ошибка " + "mergefilesjava.mergeFile() " + err.toString());
            }        
    }  
        
    /*установка кодироки для консольного приложения*/ 
    private static void setEncoding(){
        try { 
            System.setErr(new PrintStream(System.err, true, "cp866"));
            System.setOut(new PrintStream(System.out, true, "cp866"));
        } catch (UnsupportedEncodingException err){
            System.out.println("Ошибка параметров кодировки! " + "mergefilesjava.setEncoding() " + err.toString());
        }
    } 
    
    /*прогресс бар для консольного приложения*/ 
    private static int updateProgress(int progress,
                                      String str) {
        if      (str != null)    {System.out.println("\n" + str); return progress++;}        
        else if (progress > 300) {System.out.println("*");        return progress = 0;}
        else                     {System.out.print("*");          return progress++;}
    }
    
     /* вывод данных в итоговый файл*/ 
    private static void writeOutFile(File outFile, 
                                     ArrayList lines) 
                                     throws IOException {        
        BufferedWriter fbw = null;    
        if(lines.size() == 0) return;
        try { 
            fbw = new BufferedWriter(new FileWriter(outFile, true));            
                for (int i = 0; i < lines.size(); i++) {
                    fbw.write(lines.get(i).toString() + "\n");
                }       
            } catch (FileNotFoundException err) {
                  System.out.println("Ошибка! Файл не найден! " + "mergefilesjava.writeOutFile() " + err.toString());            
            } catch (IOException err) {
                  System.out.println("Ошибка записи данных! " + "mergefilesjava.writeOutFile() " + err.toString());
            } finally {
                if (fbw != null) {
                 fbw.flush();
                 fbw.close();
                }
            }  
        return;
    }  
    
    /*перезаписываем временнный файл, заполняем данными после слияния двух временных файлов*/ 
    private static void reWriteTmpFile(String fileName, 
                                       ArrayList <Comparable> tmp, 
                                       int size) throws IOException {          
        BufferedWriter fbw = null;
        try { 
            fbw = new BufferedWriter(new FileWriter(fileName));
            for (int i = size; i < tmp.size(); i++) {
                fbw.write(tmp.get(i).toString() + "\n");  
            }
            int h = tmp.size(), j = 0;
            for (int i = size; i < h; i++) {
                tmp.remove(h-1-j);
                j++;
            }
        } catch (FileNotFoundException err) {
            System.out.println("Ошибка! Файл не найден! " + "mergefilesjava.reWriteTmpFile() " + err.toString());            
        } catch (IOException err) {
            System.out.println("Ошибка перезаписи данных !" + "mergefilesjava.reWriteTmpFile() " + err.toString());
        } finally {
            if (fbw != null) {
                fbw.flush();
                fbw.close();
            }
        }              
    }   
    
    /*создание выходного файла*/
    private static File createOutFile(String filename){                
        try { 
            File outFile = new File (filename);         
            if (outFile.isFile()){   
                FileWriter fw = new FileWriter(filename);
                fw.close();     
                return outFile;
            }
            if (outFile.createNewFile()) return outFile;                           
            } catch (IOException err) {
                System.out.println("Ошибка создания выходного файла! " + "mergefilesjava.createOutFile() " + err.toString());           
            }   
        return null;  
    } 
   
    /*проверка массива входящих файлов*/
    private static void checkInFile(ArrayList a){
        BufferedReader fbr = null;
        for (int i = 0; i < a.size(); i++) {
            try { 
                fbr = new BufferedReader(new FileReader(a.get(i).toString()));                                         
            } catch (FileNotFoundException err) {
            System.out.println("Ошибка! Файл не найден! " + "mergefilesjava.checkInFile() " +  err.toString());  
            return;
            } finally {
                try {
                if (fbr != null) fbr.close();
                } catch (IOException err) {
                    System.out.println("Ошибка закрытия потока! " + "mergefilesjava.checkInFile() " + err.toString());
                }    
            }
        }       
    } 
    
    /*объем доступной памяти*/
    private static long estimateAvailableMemory() {
        //http://stackoverflow.com/questions/12807797/java-get-available-memory 
        System.gc();
        Runtime r = Runtime.getRuntime();
        long allocatedMemory = r.totalMemory() - r.freeMemory();
        long presFreeMemory  = r.maxMemory()   - allocatedMemory;
        return presFreeMemory;
    }
    
    /*определяем размер временного файла,
    который можно будет загрузить полностью в массив для его дальнейшего слияния*/
    private static long estimateBestSizeOfBlocks(final long sizeoffile,
                                                 final int  maxtmpfiles, 
                                                 final long maxMemory) {
        /*округление если деление без остатка то прибавим 0, иначе прибавим 1*/
        long blocksize = sizeoffile / maxtmpfiles + (sizeoffile % maxtmpfiles == 0 ? 0 : 1);
        if (blocksize < maxMemory / 2) blocksize = maxMemory / 2;        
        return blocksize;
    }
    
    /* https://habr.com/ru/post/134102/*/
    private static int overHead(){
        int objHeader;      /*заголовок объекта*/
        int arrHeader;      /*заголовок массива т.к. строка массив символов*/
        int intFields = 12; /*поле типа int*/
        int multiple;       /*выравнивание для кратности пока не понятно*/
        int objRef;         /*ссылка на массив*/
        int objOverhead;
        boolean is64bitJVM = true;
        String arch = System.getProperty("sun.arch.data.model");
        if (arch != null) {
            if (arch.contains("32")) is64bitJVM = false;
        }          
        objHeader = is64bitJVM ? 16 : 8;
        arrHeader = is64bitJVM ? 24 : 12;
        objRef    = is64bitJVM ? 8  : 4;        
        multiple  = is64bitJVM ? 4  : 2;  
        objOverhead = objHeader + intFields + multiple + objRef + arrHeader;
        return objOverhead;    
    }
     
    /*удаляем временные файлы*/
    private static void deleteTempFile(List<File> tmpFile) {
        int i = 0;    
        while (i < tmpFile.size()){
            tmpFile.get(i).deleteOnExit();
            i++;
        }
    }
    
    /*создаем временные файлы*/                    
    private static File saveTempFile(List<String> tmplist, 
                                     File tmpdir) {
        File newtmpfile = null;
        BufferedWriter fbw = null;
        try { 
            newtmpfile = File.createTempFile("BlockTempFile", ".tmp", tmpdir);                       
            OutputStream out = new FileOutputStream(newtmpfile);
            fbw = new BufferedWriter(new OutputStreamWriter(out, Charset.defaultCharset()));
            for (String r : tmplist) {
                fbw.write(r);
                fbw.newLine();
            }
        } catch (FileNotFoundException err) {
            System.out.println("Ошибка! Файл не найден!" +  "mergefilesjava.saveTempFile() " + err.toString());
        } catch (IOException err) {
            System.out.println("Ошибка записи временного файла! " +  "mergefilesjava.saveTempFile() " + err.toString());
        } finally {
            try {
               if (fbw != null) {
                   fbw.flush(); 
                   fbw.close();
                };                 
            } catch (IOException err) {
            System.out.println("Ошибка закрытия пока " + "mergefilesjava.saveTempFile() " + err.toString());
            }          
        }       
        return newtmpfile;
    }
    
    /*делим исходный файл на блоки, размер блока = объем доступной памяти/2, 
    каждый блок сохраняем в отдельный файл*/
    private static List<File> createBlockTempFile(final BufferedReader fbr,
                                                  final long datalength,
                                                  final int maxtmpfiles, 
                                                  long maxMemory,
                                                  final File tmpdir,
                                                  int objOverhead) throws IOException {
        List<File> files = new ArrayList<>();
        long blocksize = estimateBestSizeOfBlocks(datalength, maxtmpfiles, maxMemory);        
            List<String> tmplist = new ArrayList<>();
            String line = "";
            try {                        
                while (line != null) {
                    long currentblocksize = 0;
                    while ((currentblocksize < blocksize) && ((line = fbr.readLine()) != null)) {                                        
                        if (line.length() != 0) { 
                            tmplist.add(line);
                           /*line.length()количество элементов в строке умножаем на 2байта под хранение каждого элеиента*/
                          currentblocksize += (line.length() * 2) + objOverhead;  
                        } 
                    }
                    files.add(saveTempFile(tmplist, tmpdir));
                    tmplist.clear();                                
                }
            } catch (EOFException err) {                 
                if (tmplist.size() > 0) {
                    files.add(saveTempFile(tmplist, tmpdir));
                    tmplist.clear();
                }
            } catch (IOException err) {
                System.out.println("Ошибка " + "mergefilesjava.createBlockTempFile() " + err.toString());
            } 
        return files; 
    }
    
    /*получаем размер файла*/   
    private static long getSizeFile(String filename) {
        long sizeFile = 0;
        File outFile = new File (filename);            
        sizeFile     = outFile.length();
        return sizeFile;
    }
    
    /*сравнение двух элементов массива*/   
    private static boolean objCompare(Comparable v, 
                                      Comparable w,
                                      boolean sort) {
       if(sort) return v.compareTo(w) < 0;            
       else     return v.compareTo(w) > 0;       
    }
    
    /*проверим в какой последовательности отсортирован  массив, и отосортирован ли вообще*/
    private static boolean checkArraySort(ArrayList <Comparable> tmp,  
                                          int min,
                                          int max) {
        for (int i = min; i < max - 1; i++){
            if (objCompare(tmp.get(i), tmp.get(i+1), false)) {                 
                return true; /* по возрастанию*/
            }
        }    
        return false; 
    }
    /*
    private static boolean checkArraySort(Comparable[] a, boolean pr) {
        return isSorted(a, 0, a.length - 1, pr);
    }

    private static boolean isSorted(Comparable[] a, int lo, int hi, boolean pr) {
        for (int i = lo + 1; i <= hi; i++)
            if (less(a[i], a[i-1], pr)) return false;
        return true;
    }
    */
    /*
    private static void sortMerge(ArrayList <Comparable> tmpFile, 
                          ArrayList <Comparable> tmp, 
                          int lo, int hi,                           
                          boolean sort) {
        if (hi <= lo) return;
        int mid = lo + (hi - lo) / 2;
        sortMerge(tmpFile, tmp, lo, mid, sort);
        sortMerge(tmpFile, tmp, mid + 1, hi, sort);
        objMerge(tmpFile, tmp, lo, mid, hi, sort);                        
    }*/

    /*
    public static void sortMerge(ArrayList <Comparable> tmpFile, boolean sort) {
       ArrayList <Comparable> tmp = new ArrayList <Comparable>();
       sortMerge(tmpFile, tmp, 0, tmpFile.size()- 1, sort);       
    }
     */  
    
    /*слияние массива*/      
    private static void objMerge(ArrayList <Comparable> tmpFile,
                                 int size,
                                 boolean sort) {
                                    
        ArrayList <Comparable> tmp = new ArrayList <Comparable>();  
        int hi = tmpFile.size();            
        int i = 0;             
        int j = size;                                                                  
        try {
            if (checkArraySort(tmpFile, 0, size) == sort){          
                for (int k = size - 1; k >= 0; k--) tmp.add(tmpFile.get(k));     
            } else { 
                for (int k = 0; k < size; k++) tmp.add(tmpFile.get(k));
            }            
            if (checkArraySort(tmpFile, size, hi) == sort){
                for (int k = hi - 1; k >= size; k--) tmp.add(tmpFile.get(k));     
            } else {
                for (int k = size; k < hi; k++)  tmp.add(tmpFile.get(k));
            }
            for (int k = 0; k < hi; k++) {
                if      (i >= size)                                {tmpFile.set(k, tmp.get(j)); j++;} 
                else if (j >= hi)                                  {tmpFile.set(k, tmp.get(i)); i++;}       
                else if (objCompare(tmp.get(j), tmp.get(i), sort)) {tmpFile.set(k, tmp.get(j)); j++;}     
                else                                               {tmpFile.set(k, tmp.get(i)); i++;}            
            }
                          
            /* слияние в обратном порядке
            i = size - 1;
            j = hi - 1;
            for (int k = 0; k < hi; k++) {
                if      (i == 0)                                  {tmpFile.set(k, tmp.get(j)); j--;} 
                else if (j < size)                                {tmpFile.set(k, tmp.get(i)); i--;}       
                else if (objCompare(tmp.get(j), tmp.get(i)))      {tmpFile.set(k, tmp.get(i)); i--;}       
                else                                              {tmpFile.set(k, tmp.get(j)); j--;}            
            }*/
             
        } catch (Exception err) {
           System.out.println("Ошибка " + "mergefilesjava.objMerge() " + err.toString());
        }        
    }
    
    /*Обходим массив файлов с исходными данными и формируем список временных файлов 
    допустимого размера для загрузки в оперативную память в целях дальнейшего слияния*/
    private static List<File> readInFile(ArrayList inFile) throws IOException {       
        int maxtmpfiles    = 1024;
        int progress       = 0; 
        BufferedReader fbr = null;        
        File tmpdir        = new File("").getAbsoluteFile();    
        List<File> tmpFile = new ArrayList<>();
        checkInFile(inFile);    
        int objOverhead    = overHead();      
        try {
            progress = updateProgress(progress, "Формируем список временных файлов: ");
            for (int i = 0; i < inFile.size(); i++) {
                fbr = new BufferedReader(new InputStreamReader(new FileInputStream(inFile.get(i).toString()), Charset.defaultCharset()));            
                List<File> tmpList = createBlockTempFile(fbr, getSizeFile(inFile.get(i).toString()), maxtmpfiles, estimateAvailableMemory(), tmpdir, objOverhead);                          
                for (File f : tmpList) tmpFile.add(f);                                       
                progress = updateProgress(progress, null);
            }
        } catch (FileNotFoundException err) {
                System.out.println("Ошибка! Файл не найден!" + "mergefilesjava.readInFile() " + err.toString());  
        } catch (Exception err) {    
                System.out.println("Ошибка! " + "mergefilesjava.readInFile() " + err.toString());             
        } finally {
                try {
                     if (fbr != null) fbr.close();
                    } catch (IOException err) {
                    System.out.println("Ошибка закрытия потока " + "mergefilesjava.readInFile() " + err.toString());
                    }             
        } 
        return tmpFile;     
    }    
    
    /*Обходим список временных файлов читаем их целиком в массив и осуществляем процедуру их слияния*/    
    private static void mergeFile(ArrayList inFile, 
                                  String outNameFile,
                                  boolean sort,
                                  boolean type) 
                                  throws IOException, FileNotFoundException {     
        int i = 0, j = 0;
        int progress = 0;
        String line = null; 
        List<File> tmpFile = readInFile(inFile);  
        File outFile       = createOutFile(outNameFile);
        progress = updateProgress(progress, "Выполняется процедура слияния временных файлов: ");        
        while (i < tmpFile.size()){             
            FileReader fileReader = new FileReader(tmpFile.get(i).getPath());
            BufferedReader fbr    = new BufferedReader(fileReader);           
            ArrayList <Comparable> lines = new ArrayList <Comparable>();
            line = null;
            while ((line = fbr.readLine()) != null) {    
                if (line.length() != 0) {
                    if (type) lines.add(line);     
                    else      lines.add(Integer.parseInt(line));                
                }
            }
            fbr.close();
            fileReader.close();
            line = null; 
            j = i + 1;               
            while (j < tmpFile.size()){              
                fileReader = new FileReader(tmpFile.get(j).getPath());
                fbr        = new BufferedReader(fileReader);                         
                int size   = lines.size();
                while ((line = fbr.readLine()) != null) {
                    if (line.length() != 0) {
                        if (type) lines.add(line);     
                        else      lines.add(Integer.parseInt(line));  
                    }
                }
                fbr.close();
                fileReader.close();                
                objMerge(lines, size, sort);                  
                reWriteTmpFile(tmpFile.get(j).getPath(), lines, size);        
                progress = updateProgress(progress, null);
                j++;               
            }        
        writeOutFile(outFile, lines);
        progress = updateProgress(progress, null);
        i++;        
        }      
        deleteTempFile(tmpFile);          
    }
}    



