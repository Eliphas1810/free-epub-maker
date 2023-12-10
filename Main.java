import javax.swing.JFrame;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.io.PrintWriter;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

public class Main {

    //HTMLエスケープ
    //HTMLエスケープは'を&#39;に置換
    private static String escapeHtml(String string) {
        string = string.replaceAll("&", "&amp;");
        string = string.replaceAll("\"", "&quot;");
        string = string.replaceAll("'", "&#39;");
        string = string.replaceAll("<", "&lt;");
        string = string.replaceAll(">", "&gt;");
        return string;
    }

    //XMLエスケープ
    //XMLエスケープは'を&apos;に置換
    private static String escapeXml(String string) {
        string = string.replaceAll("&", "&amp;");
        string = string.replaceAll("\"", "&quot;");
        string = string.replaceAll("'", "&apos;");
        string = string.replaceAll("<", "&lt;");
        string = string.replaceAll(">", "&gt;");
        return string;
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();
        frame.setTitle("電子書籍(ePub)作成"); //ウィンドウのタイトルバーのタイトル
        frame.setSize(800, 300); //初期表示のウィンドウの横幅と高さ
        frame.setLocationRelativeTo(null); //モニターの中央に表示
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //ウィンドウ右上のXボタンで終了

        Container container = frame.getContentPane();
        container.setLayout(null); //LayoutManagerを使わず座標を指定して自由に配置

        JLabel inDirLabel = new JLabel("");
        inDirLabel.setSize(10, 10);
        inDirLabel.setLocation(10, 40); //座標を指定
        container.add(inDirLabel);

        JButton inDirSelectButton = new JButton("読み込みディレクトリ選択");
        inDirSelectButton.setSize(240, 20);
        inDirSelectButton.setLocation(10, 10); //座標を指定
        container.add(inDirSelectButton);

        //読み込みディレクトリ選択ボタンが押された時の処理
        inDirSelectButton.addActionListener(
            actionEvent -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(container) == JFileChooser.APPROVE_OPTION) {
                    inDirLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
                    inDirLabel.setSize(inDirLabel.getText().length() * 20, 10);
                }
            }
        );

        JLabel outDirLabel = new JLabel("");
        outDirLabel.setSize(10, 10);
        outDirLabel.setLocation(10, 90); //座標を指定
        container.add(outDirLabel);

        JButton outDirSelectButton = new JButton("書き込みディレクトリ選択");
        outDirSelectButton.setSize(240, 20);
        outDirSelectButton.setLocation(10, 60); //座標を指定
        container.add(outDirSelectButton);

        //書き込みディレクトリ選択ボタンが押された時の処理
        outDirSelectButton.addActionListener(
            actionEvent -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(container) == JFileChooser.APPROVE_OPTION) {
                    outDirLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
                    outDirLabel.setSize(outDirLabel.getText().length() * 20, 10);
                }
            }
        );

        JLabel authorLabel = new JLabel("著者");
        authorLabel.setSize(40, 10);
        authorLabel.setLocation(10, 115); //座標を指定
        container.add(authorLabel);

        JTextField authorTextField = new JTextField(80);
        authorTextField.setSize(160, 20);
        authorTextField.setLocation(50, 110); //座標を指定
        container.add(authorTextField);

        JButton makeEPubButton = new JButton("電子書籍(ePub)作成");
        makeEPubButton.setSize(240, 20);
        makeEPubButton.setLocation(10, 140); //座標を指定
        container.add(makeEPubButton);

        //電子書籍(ePub)作成ボタンが押された時の処理
        makeEPubButton.addActionListener(actionEvent -> {
            try {

                String inDir = inDirLabel.getText();
                String outDir = outDirLabel.getText();
                String author = authorTextField.getText();

                if (inDir.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "読み込みディレクトリを選択してください。");
                    return;
                }

                if (outDir.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "書き込みディレクトリを選択してください。");
                    return;
                }

                if (author.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "著者を記入してください。");
                    return;
                }

                if (Files.exists(Paths.get(inDir)) == false) {
                    JOptionPane.showMessageDialog(frame, "読み込みディレクトリを選択し直してください。");
                    return;
                }

                if (Files.exists(Paths.get(outDir)) == false) {
                    JOptionPane.showMessageDialog(frame, "書き込みディレクトリを選択し直してください。");
                    return;
                }

                //XMLの.opfファイルの出版日としても利用する、XMLの.opfファイルの最終更新日時
                String epubMakeDateTimeXml = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

                Path inDirPath = Paths.get(inDir);

                String title = inDirPath.getFileName().toString();

                Path[] fileAndDirPathArray = Files.list(inDirPath).toArray(Path[]::new);

                Path[] textPathArray = Stream.of(fileAndDirPathArray).filter(path -> Files.isDirectory(path) == false && path.getFileName().toString().matches("^.+\\.[tT][xX][tT]$")).sorted().toArray(Path[]::new);

                Path[] imagePathArray = Stream.of(fileAndDirPathArray).filter(path -> Files.isDirectory(path) == false && path.getFileName().toString().matches("^.+\\.[jJ][pP][eE]?[gG]$|^.+\\.[pP][nN][gG]$")).sorted().toArray(Path[]::new);

                Path coverImagePath = null;
                for (Path path : imagePathArray) {
                    if (path.getFileName().toString().matches("^cover\\.[jJ][pP][eE]?[gG]$|^cover\\.[pP][nN][gG]$")) {
                        coverImagePath = path;
                        break;
                    }
                }

                if (textPathArray.length == 0) {
                    JOptionPane.showMessageDialog(frame, "読み込みディレクトリに.txtファイルが有りません。");
                    return;
                }

                //一時ディレクトリを作成
                Path tempDirPath = Files.createTempDirectory(null);

                //mimetypeファイルを新規作成
                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempDirPath.resolve("mimetype")))) {
                    printWriter.print("application/epub+zip");
                }

                //META-INFディレクトリを新規作成

                Path metaInfDirPath = tempDirPath.resolve("META-INF");

                Files.createDirectory(metaInfDirPath);

                //META-INF/container.xmlを新規作成
                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(metaInfDirPath.resolve("container.xml")))) {
                    printWriter.print(String.join("\n",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                        "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">",
                        "    <rootfiles>",
                        "        <rootfile full-path=\"opf.opf\" media-type=\"application/oebps-package+xml\"/>",
                        "    </rootfiles>",
                        "</container>"
                    ));
                }

                //opf.opfを新規作成
                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempDirPath.resolve("opf.opf")))) {
                    printWriter.print(String.join("\n",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                        "<package unique-identifier=\"pub-id\" version=\"3.0\" xmlns=\"http://www.idpf.org/2007/opf\">",
                        "    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">",
                        "        <dc:identifier id=\"pub-id\">urn:uuid:" + UUID.randomUUID().toString().toUpperCase() + "</dc:identifier><!-- UUID -->",
                        "        <dc:title>" + escapeXml(title) + "</dc:title>",
                        "        <dc:language>ja</dc:language>",
                        "        <meta property=\"dcterms:modified\">" + epubMakeDateTimeXml + "</meta><!-- 最終更新日時 -->",
                        "\n",
                        "        <dc:creator id=\"creator1\">" + escapeXml(author) + "</dc:creator><!-- 著者の名前 -->",
                        "        <meta refines=\"#creator1\" property=\"role\" scheme=\"marc:relators\" id=\"role\">aut</meta>",
                        "\n",
                        "        <dc:publisher>" + escapeXml(author) + "</dc:publisher><!-- 出版者か出版社 -->",
                        "        <dc:date>" + epubMakeDateTimeXml + "</dc:date><!-- 出版日 -->",
                        "    </metadata>",
                        "    <manifest>",
                        "        <item id=\"css\" href=\"common.css\" media-type=\"text/css\"/>",
                        "        <item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n"
                    ));

                    //cover.jpgかcover.pngが存在する場合
                    if (coverImagePath != null) {
                        //cover.jpgの場合
                        if (coverImagePath.getFileName().toString().matches("^cover\\.[jJ][pP][eE]?[gG]$")) {
                            printWriter.print("        <item id=\"cover\" href=\"" + coverImagePath.getFileName() + "\" media-type=\"image/jpeg\" properties=\"cover-image\"/>\n");
                        //cover.pngの場合
                        } else {
                            printWriter.print("        <item id=\"cover\" href=\"" + coverImagePath.getFileName() + "\" media-type=\"image/png\" properties=\"cover-image\"/>\n");
                        }
                    }

                    //.txtファイルの数だけ、くり返し
                    for (int index = 0; index < textPathArray.length; index++) {
                        printWriter.print("        <item id=\"index" + (index + 1) + "\" href=\"index" + (index + 1) + ".xhtml\" media-type=\"application/xhtml+xml\"/>\n");
                    }

                    //画像ファイルの数だけ、くり返し

                    //cover.jpg以外のjpeg画像ファイルの数だけ、くり返し
                    Stream.of(imagePathArray)
                        .filter(path -> path.getFileName().toString().matches("^cover\\.[jJ][pP][eE]?[gG]$") == false)
                        .filter(path -> path.getFileName().toString().matches("^.+\\.[jJ][pP][eE]?[gG]$"))
                        .forEach(path -> {
                            printWriter.print("        <item id=\"" + escapeXml(path.getFileName().toString().replaceFirst("\\.[jJ][pP][eE]?[gG]$", "")) + "\" href=\"" + escapeXml(path.getFileName().toString()) + "\" media-type=\"image/jpeg\"/>\n");
                        }
                    );

                    //cover.png以外のpng画像ファイルの数だけ、くり返し
                    Stream.of(imagePathArray)
                        .filter(path -> path.getFileName().toString().matches("^cover\\.[pP][nN][gG]$") == false)
                        .filter(path -> path.getFileName().toString().matches("^.+\\.[pP][nN][gG]$"))
                        .forEach(path -> {
                            printWriter.print("        <item id=\"" + escapeXml(path.getFileName().toString().replaceFirst("\\.[pP][nN][gG]$", "")) + "\" href=\"" + escapeXml(path.getFileName().toString()) + "\" media-type=\"image/png\"/>\n");
                        }
                    );

                    printWriter.print("    </manifest>\n");
                    printWriter.print("    <spine>\n");

                    for (int index = 0; index < textPathArray.length; index++) {
                        printWriter.print("        <itemref idref=\"index" + (index + 1) + "\"/>\n");
                    }

                    printWriter.print("    </spine>\n");
                    printWriter.print("</package>\n");
                }

                //common.cssファイルを新規作成
                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempDirPath.resolve("common.css")))) {
                    printWriter.print(String.join("\n",
                        "h1 {",
                        "    margin: 0;",
                        "    display: block;",
                        "    width: 100%;",
                        "    text-align: center;",
                        "    font-size: 2rem;",
                        "    font-weight: bold;",
                        "}",
                        "",
                        "h2 {",
                        "    margin: 0;",
                        "    margin-bottom: 1.5rem;",
                        "    page-break-before: always;",
                        "    display: block;",
                        "    width: 100%;",
                        "    text-align: center;",
                        "    font-size: 1.5rem;",
                        "    font-weight: bold;",
                        "}",
                        "",
                        "p {",
                        "    margin: 0;",
                        "}",
                        "",
                        "img {",
                        "    display: block;",
                        "    height: 90%;",
                        "    margin: auto;",
                        "}\n"
                    ));
                }


                //nav.xhtmlファイルを新規作成
                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempDirPath.resolve("nav.xhtml")))) {
                    printWriter.print(String.join("\n",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" lang=\"ja\" xml:lang=\"ja\">",
                        "    <head>",
                        "        <title>目次</title>",
                        "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>",
                        "    </head>",
                        "    <body>",
                        "        <nav epub:type=\"toc\">",
                        "            <ol>\n"
                    ));

                    //.txtファイルの数だけ、くり返し
                    for (int index = 0; index < textPathArray.length; index++) {
                        printWriter.print("                <li><a href=\"index" + (index + 1) + ".xhtml#episode" + (index + 1) + "\">" + escapeHtml(textPathArray[index].getFileName().toString().replaceAll("^[0-9]*[ 　]*|\\.[tT][xX][tT]$", "")) + "</a></li>\n");
                    }

                    printWriter.print(String.join("\n",
                        "            </ol>",
                        "        </nav>",
                        "    </body>",
                        "</html>"
                    ));
                }

                //画像ファイルを読み込みディレクトリから一時ディレクトリへコピー
                Stream.of(imagePathArray).forEach(path -> {
                    try {
                        Files.copy(path, tempDirPath.resolve(path.getFileName()));
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });


                //.txtファイルの数だけ、くり返し
                for (int index = 0; index < textPathArray.length; index++) {
                    try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempDirPath.resolve("index" + (index + 1) + ".xhtml")))) {

                        printWriter.print(String.join("\n",
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" lang=\"ja\" xml:lang=\"ja\">",
                            "    <head>",
                            "        <title>" + escapeHtml(title) + "</title>",
                            "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>",
                            "        <link href=\"common.css\" rel=\"stylesheet\" type=\"text/css\"/>",
                            "    </head>",
                            "    <body>\n"
                        ));

                        if (index == 0) {
                            printWriter.print("        <h1>" + escapeHtml(title) + "</h1>\n");
                        }

                        printWriter.print("        <h2 id=\"episode" + (index + 1) + "\">" + escapeHtml(textPathArray[index].getFileName().toString().replaceAll("^[0-9]*[ 　]*|\\.[tT][xX][tT]$", "")) + "</h2>\n");

                        try (BufferedReader bufferedReader = Files.newBufferedReader(textPathArray[index])) {
                            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {

                                //読み込んだ行が.jpgか.pngで終わる場合
                                if (line.matches("^.+\\.[jJ][pP][eE]?[gG]$|^.+\\.[pP][nN][gG]$")) {
                                    //imgタグに置換
                                    printWriter.print("        <img id=\"" + escapeHtml(line.replaceFirst("\\.[jJ][pP][eE]?[gG]$|\\.[pP][nN][gG]$", "")) + "\" src=\"" + escapeHtml(line) + "\" />\n");
                                 //読み込んだ行が空行の場合
                                 } else if (line.isEmpty()) {
                                     //<p>全角空白</p>を出力
                                     printWriter.print("        <p>　</p>\n");
                                 //読み込んだ行が普通のテキストの場合
                                 } else {
                                     //HTMLエスケープ
                                     String text = escapeHtml(line);

                                     //漢字 半角括弧開き ひらがなかカタカナ 半角括弧閉じ をルビに置換
                                     text = text.replaceAll("([一-鿋々]+)\\(([ぁ-ゖァ-ヺー]+)\\)", "<ruby>$1<rt>$2</rt></ruby>");

                                     //全角縦線 二重山括弧開き以外の文字 二重山括弧開き 二重山括弧閉じ以外の文字 二重山括弧閉じ をルビに置換
                                     text = text.replaceAll("｜([^《]+)《([^》]+)》", "<ruby>$1<rt>$2</rt></ruby>");

                                     //漢字 二重山括弧開き ひらがなかカタカナ 二重山括弧閉じ をルビに置換
                                     text = text.replaceAll("([一-鿋々]+)《([ぁ-ゖァ-ヺー]+)》", "<ruby>$1<rt>$2</rt></ruby>");

                                     printWriter.print("        <p>" + text + "</p>\n");
                                }
                            }
                        }

                        printWriter.print("    </body>\n");
                        printWriter.print("</html>\n");
                    }
                }

                try(
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(outDir).resolve(title + ".epub")));
        		    ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);
                ) {
                    zipOutputStream.setMethod(ZipOutputStream.STORED);
                    CRC32 crc32 = new CRC32(); //ZipOutputStream.STOREDの場合は必要

                    ZipEntry mimetypeZipEntry = new ZipEntry("mimetype");
                    mimetypeZipEntry.setSize(Files.size(tempDirPath.resolve("mimetype"))); //ZipOutputStream.STOREDの場合は必要
                    //crc32.reset();
                    crc32.update(Files.readAllBytes(tempDirPath.resolve("mimetype"))); //ZipOutputStream.STOREDの場合は必要
                    mimetypeZipEntry.setCrc(crc32.getValue()); //ZipOutputStream.STOREDの場合は必要
                    zipOutputStream.putNextEntry(mimetypeZipEntry);
                    zipOutputStream.write(Files.readAllBytes(tempDirPath.resolve("mimetype")));

                    ZipEntry containerZipEntry = new ZipEntry("META-INF/container.xml");
                    containerZipEntry.setSize(Files.size(metaInfDirPath.resolve("container.xml")));
                    crc32.reset();
                    crc32.update(Files.readAllBytes(metaInfDirPath.resolve("container.xml")));
                    containerZipEntry.setCrc(crc32.getValue());	
                    zipOutputStream.putNextEntry(containerZipEntry);
                    zipOutputStream.write(Files.readAllBytes(metaInfDirPath.resolve("container.xml")));

                    ZipEntry opfZipEntry = new ZipEntry("opf.opf");
                    opfZipEntry.setSize(Files.size(tempDirPath.resolve("opf.opf")));
                    crc32.reset();
                    crc32.update(Files.readAllBytes(tempDirPath.resolve("opf.opf")));
                    opfZipEntry.setCrc(crc32.getValue());	
                    zipOutputStream.putNextEntry(opfZipEntry);
                    zipOutputStream.write(Files.readAllBytes(tempDirPath.resolve("opf.opf")));

                    ZipEntry cssZipEntry = new ZipEntry("common.css");
                    cssZipEntry.setSize(Files.size(tempDirPath.resolve("common.css")));
                    crc32.reset();
                    crc32.update(Files.readAllBytes(tempDirPath.resolve("common.css")));
                    cssZipEntry.setCrc(crc32.getValue());	
                    zipOutputStream.putNextEntry(cssZipEntry);
                    zipOutputStream.write(Files.readAllBytes(tempDirPath.resolve("common.css")));

                    ZipEntry navZipEntry = new ZipEntry("nav.xhtml");
                    navZipEntry.setSize(Files.size(tempDirPath.resolve("nav.xhtml")));
                    crc32.reset();
                    crc32.update(Files.readAllBytes(tempDirPath.resolve("nav.xhtml")));
                    navZipEntry.setCrc(crc32.getValue());	
                    zipOutputStream.putNextEntry(navZipEntry);
                    zipOutputStream.write(Files.readAllBytes(tempDirPath.resolve("nav.xhtml")));

                    Stream.of(imagePathArray).forEach(path -> {
                        try {

                            ZipEntry imageZipEntry = new ZipEntry(path.getFileName().toString());
                            imageZipEntry.setSize(Files.size(tempDirPath.resolve(path.getFileName())));
                            crc32.reset();
                            crc32.update(Files.readAllBytes(tempDirPath.resolve(path.getFileName())));
                            imageZipEntry.setCrc(crc32.getValue());	
                            zipOutputStream.putNextEntry(imageZipEntry);
                            zipOutputStream.write(Files.readAllBytes(tempDirPath.resolve(path.getFileName())));

                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    });

                    for (int index = 0; index < textPathArray.length; index++) {
                        ZipEntry textZipEntry = new ZipEntry("index" + (index + 1) + ".xhtml");
                        textZipEntry.setSize(Files.size(tempDirPath.resolve("index" + (index + 1) + ".xhtml")));
                        crc32.reset();
                        crc32.update(Files.readAllBytes(tempDirPath.resolve("index" + (index + 1) + ".xhtml")));
                        textZipEntry.setCrc(crc32.getValue());	
                        zipOutputStream.putNextEntry(textZipEntry);
                        zipOutputStream.write(Files.readAllBytes(tempDirPath.resolve("index" + (index + 1) + ".xhtml")));
                    }
                }

                Files.delete(tempDirPath.resolve("mimetype"));
                Files.delete(tempDirPath.resolve("META-INF/container.xml"));
                Files.delete(tempDirPath.resolve("META-INF"));
                Files.delete(tempDirPath.resolve("opf.opf"));
                Files.delete(tempDirPath.resolve("common.css"));
                Files.delete(tempDirPath.resolve("nav.xhtml"));

                Stream.of(imagePathArray).forEach(path -> {
                    try {
                        Files.delete(tempDirPath.resolve(path.getFileName()));
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });

                for (int index = 0; index < textPathArray.length; index++) {
                    Files.delete(tempDirPath.resolve("index" + (index + 1) + ".xhtml"));
                }

                Files.delete(tempDirPath);

                JOptionPane.showMessageDialog(frame, "電子書籍(ePub)の作成が完了しました。");

            } catch (Exception exception) {
                JOptionPane.showMessageDialog(frame, "エラー: " + exception);
                throw new RuntimeException(exception);
            }
        });

        frame.setVisible(true); //ウィンドウを表示
    }
}
