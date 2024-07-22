import tkinter
from tkinter import filedialog
from tkinter import messagebox
import os
import re
import tempfile
import datetime
import uuid
import shutil
import zipfile

root = tkinter.Tk()
root.geometry("800x300")
root.title("電子書籍(ePub)作成")

in_dir_string_var = tkinter.StringVar()
in_dir_string_var.set("")

in_dir_label = tkinter.Label(root, textvariable=in_dir_string_var)
in_dir_label.place(x=10, y=40)

# 読み込みディレクトリ選択ボタンが押された時の処理
def select_in_dir():
    try:
        in_dir_string_var.set(filedialog.askdirectory(initialdir=os.path.expanduser("~")))
    except Exception as exception:
        messagebox.showerror(exception.__class__.__name__, str(exception))
        raise

in_dir_select_button = tkinter.Button(root, text="読み込みディレクトリ選択", command=select_in_dir)
in_dir_select_button.place(x=10, y=10)

out_dir_string_var = tkinter.StringVar()
out_dir_string_var.set("")

out_dir_label = tkinter.Label(root, textvariable=out_dir_string_var)
out_dir_label.place(x=10, y=110)

# 書き込みディレクトリ選択ボタンが押された時の処理
def select_out_dir():
    try:
        out_dir_string_var.set(filedialog.askdirectory(initialdir=os.path.expanduser("~")))
    except Exception as exception:
        messagebox.showerror(exception.__class__.__name__, str(exception))
        raise

out_dir_select_button = tkinter.Button(root, text="書き込みディレクトリ選択", command=select_out_dir)
out_dir_select_button.place(x=10, y=80)

author_label = tkinter.Label(text="著者")
author_label.place(x=10, y=150)

author_textbox = tkinter.Entry(width=80)
author_textbox.place(x=50, y=150)

message_string_var = tkinter.StringVar()
message_string_var.set("")

message_label = tkinter.Label(root, textvariable=message_string_var)
message_label.place(x=10, y=220)

# HTMLエスケープ
# HTMLエスケープは'を&#39;に置換
def escape_html(text):
    text = re.sub("&", "&amp;", text)
    text = re.sub('"', "&quot;", text)
    text = re.sub("'", "&#39;", text)
    text = re.sub("<", "&lt;", text)
    text = re.sub(">", "&gt;", text)
    return text

# XMLエスケープ
# XMLエスケープは'を&apos;に置換
def escape_xml(text):
    text = re.sub("&", "&amp;", text)
    text = re.sub('"', "&quot;", text)
    text = re.sub("'", "&apos;", text)
    text = re.sub("<", "&lt;", text)
    text = re.sub(">", "&gt;", text)
    return text

# 電子書籍(ePub)作成ボタンが押された時の処理
def make_epub():
    try:
        in_dir = in_dir_string_var.get()
        out_dir = out_dir_string_var.get()
        author = author_textbox.get()

        if in_dir == "":
            message_string_var.set("読み込みディレクトリを選択してください。")
            return
        if out_dir == "":
            message_string_var.set("書き込みディレクトリを選択してください。")
            return
        if author == "":
            message_string_var.set("著者を記入してください。")
            return
        if os.path.exists(in_dir) == False:
            message_string_var.set("読み込みディレクトリを選択し直してください。")
            return
        if os.path.exists(out_dir) == False:
            message_string_var.set("書き込みディレクトリを選択し直してください。")
            return

        # .opfファイルの最終更新日時と出版日
        epub_make_datetime_xml = datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")

        title = os.path.basename(in_dir)

        file_name_list = os.listdir(in_dir)
        text_file_name_list = []
        image_file_name_list = []
        cover_image_file_name = ""
        for file_name in file_name_list:
            # ファイルの場合
            if os.path.isfile(os.path.join(in_dir, file_name)):
                # .txtファイルの場合
                if re.match("^.+\.[tT][xX][tT]$", file_name):
                    text_file_name_list.append(file_name)
                # .jpgファイルか.pngファイルの場合
                elif re.match("^.+\.[jJ][pP][eE]?[gG]$|^.+\.[pP][nN][gG]$", file_name):
                    image_file_name_list.append(file_name)
                    # cover.jpgかcover.pngの場合
                    if re.match("^cover\.[jJ][pP][eE]?[gG]$|^cover\.[pP][nN][gG]$", file_name):
                        cover_image_file_name = file_name

        text_file_name_list.sort()
        image_file_name_list.sort()

        if len(text_file_name_list) == 0:
            message_string_var.set("読み込みディレクトリに.txtファイルが有りません。")
            return

        # 一時ディレクトリを作成
        temp_dir = tempfile.TemporaryDirectory()

        # mimetypeファイルを新規作成
        with open(os.path.join(temp_dir.name, "mimetype"), "w", encoding="utf-8", newline="\n") as file:
            file.write('application/epub+zip')

        # META-INFディレクトリを新規作成
        meta_inf_dir = os.path.join(temp_dir.name, "META-INF")
        os.mkdir(meta_inf_dir)

        # META-INF/container.xmlを新規作成
        with open(os.path.join(meta_inf_dir, "container.xml"), "w", encoding="utf-8", newline="\n") as file:
            file.write('<?xml version="1.0" encoding="UTF-8"?>\n')
            file.write('<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">\n')
            file.write('    <rootfiles>\n')
            file.write('        <rootfile full-path="opf.opf" media-type="application/oebps-package+xml"/>\n')
            file.write('    </rootfiles>\n')
            file.write('</container>\n')

        # opf.opfファイルを新規作成
        with open(os.path.join(temp_dir.name, "opf.opf"), "w", encoding="utf-8", newline="\n") as file:
            file.write('<?xml version="1.0" encoding="UTF-8"?>\n')
            file.write('<package unique-identifier="pub-id" version="3.0" xmlns="http://www.idpf.org/2007/opf">\n')
            file.write('    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">\n')
            file.write('        <dc:identifier id="pub-id">urn:uuid:' + str(uuid.uuid4()).upper() + '</dc:identifier><!-- UUID -->\n')
            file.write('        <dc:title>' + escape_xml(title) + '</dc:title>\n')
            file.write('        <dc:language>ja</dc:language>\n')
            file.write('        <meta property="dcterms:modified">' + epub_make_datetime_xml + '</meta><!-- 最終更新日時 -->\n')
            file.write('\n')
            file.write('        <dc:creator id="creator1">' + escape_xml(author) + '</dc:creator><!-- 著者の名前 -->\n')
            file.write('        <meta refines="#creator1" property="role" scheme="marc:relators" id="role">aut</meta>\n')
            file.write('\n')
            file.write('        <dc:publisher>' + escape_xml(author) + '</dc:publisher><!-- 出版者か出版社 -->\n')
            file.write('        <dc:date>' + epub_make_datetime_xml + '</dc:date><!-- 出版日 -->\n')
            file.write('    </metadata>\n')
            file.write('    <manifest>\n')
            file.write('        <item id="css" href="common.css" media-type="text/css"/>\n')
            file.write('        <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>\n')
            # cover.jpgかcover.pngが存在する場合
            if cover_image_file_name != "":
                # cover.jpgの場合
                if re.match("^cover\.[jJ][pP][eE]?[gG]$", cover_image_file_name):
                    file.write('        <item id="cover" href="' + cover_image_file_name + '" media-type="image/jpeg" properties="cover-image"/>\n')
                # cover.pngの場合
                elif re.match("^cover\.[pP][nN][gG]$", cover_image_file_name):
                    file.write('        <item id="cover" href="' + cover_image_file_name + '" media-type="image/png" properties="cover-image"/>\n')
            # .txtファイルの数だけ、くり返し
            for number in range(1, len(text_file_name_list) + 1):
                file.write('        <item id="index' + str(number) + '" href="index' + str(number) + '.xhtml" media-type="application/xhtml+xml"/>\n')
            # 画像ファイルの数だけ、くり返し
            for image_file_name in image_file_name_list:
                # cover.jpgではない場合かcover.pngではない場合
                if image_file_name != cover_image_file_name:
                    # .jpgファイルの場合
                    if re.match("^.+\.[jJ][pP][eE]?[gG]$", image_file_name):
                        file.write('        <item id="' + escape_xml(os.path.splitext(image_file_name)[0]) + '" href="' + escape_xml(image_file_name) + '" media-type="image/jpeg"/>\n')
                    # .pngファイルの場合
                    elif re.match("^.+\.[pP][nN][gG]$", image_file_name):
                        file.write('        <item id="' + escape_xml(os.path.splitext(image_file_name)[0]) + '" href="' + escape_xml(image_file_name) + '" media-type="image/png"/>\n')
            file.write('    </manifest>\n')
            file.write('    <spine>\n')
            # .txtファイルの数だけ、くり返し
            for number in range(1, len(text_file_name_list) + 1):
                file.write('        <itemref idref="index' + str(number) + '"/>\n')
            file.write('    </spine>\n')
            file.write('</package>\n')

        # common.cssファイルを新規作成
        with open(os.path.join(temp_dir.name, "common.css"), "w", encoding="utf-8", newline="\n") as file:
            file.write('h1 {\n')
            file.write('    margin: 0;\n')
            file.write('    display: block;\n')
            file.write('    width: 100%;\n')
            file.write('    text-align: center;\n')
            file.write('    font-size: 2rem;\n')
            file.write('    font-weight: bold;\n')
            file.write('}\n')
            file.write('\n')
            file.write('h2 {\n')
            file.write('    margin: 0;\n')
            file.write('    margin-bottom: 1.5rem;\n')
            file.write('    page-break-before: always;\n')
            file.write('    display: block;\n')
            file.write('    width: 100%;\n')
            file.write('    text-align: center;\n')
            file.write('    font-size: 1.5rem;\n')
            file.write('    font-weight: bold;\n')
            file.write('}\n')
            file.write('\n')
            file.write('p {\n')
            file.write('    margin: 0;\n')
            file.write('}\n')
            file.write('\n')
            file.write('img {\n')
            file.write('    display: block;\n')
            file.write('    height: 90%;\n')
            file.write('    margin: auto;\n')
            file.write('}\n')

        # nav.xhtmlファイルを新規作成
        with open(os.path.join(temp_dir.name, "nav.xhtml"), "w", encoding="utf-8", newline="\n") as file:
            file.write('<?xml version="1.0" encoding="UTF-8"?>\n')
            file.write('<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="ja" xml:lang="ja">\n')
            file.write('    <head>\n')
            file.write('        <title>目次</title>\n')
            file.write('        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>\n')
            file.write('    </head>\n')
            file.write('    <body>\n')
            file.write('        <nav epub:type="toc">\n')
            file.write('            <ol>\n')
            # .txtファイルの数だけ、くり返し
            for index, text_file_name in enumerate(text_file_name_list):
                file.write('                <li><a href="index' + str(index + 1) + '.xhtml#episode' + str(index + 1) + '">' + escape_html(re.sub("^[0-9]*[ 　]*|\.[tT][xX][tT]$", "", text_file_name)) + '</a></li>\n')
            file.write('            </ol>\n')
            file.write('        </nav>\n')
            file.write('    </body>\n')
            file.write('</html>\n')

        # 画像ファイルを読み込みディレクトリから一時ディレクトリへコピー
        for image_file_name in image_file_name_list:
            shutil.copyfile(os.path.join(in_dir, image_file_name), os.path.join(temp_dir.name, image_file_name))

        # index番号.xhtmlファイルを作成
        # .txtファイルの数だけ、くり返し
        for index, text_file_name in enumerate(text_file_name_list):
            with open(os.path.join(temp_dir.name, "index" + str(index + 1) + ".xhtml"), "w", encoding="utf-8", newline="\n") as file:
                file.write('<?xml version="1.0" encoding="UTF-8"?>\n')
                file.write('<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="ja" xml:lang="ja">\n')
                file.write('    <head>\n')
                file.write('        <title>' + escape_html(title) + '</title>\n')
                file.write('        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>\n')
                file.write('        <link href="common.css" rel="stylesheet" type="text/css"/>\n')
                file.write('    </head>\n')
                file.write('    <body>\n')

                if index == 0:
                    file.write('        <h1>' + escape_html(title) + '</h1>\n')

                file.write('        <h2 id="episode' + str(index + 1) + '">' + escape_html(re.sub("^[0-9]*[ 　]*|\.[tT][xX][tT]$", "", text_file_name)) + '</h2>\n')
                with open(os.path.join(in_dir, text_file_name), "r", encoding="utf-8") as f:
                    while True:
                        text = f.readline()
                        if text == '':
                            break
                        # 文末の改行コードを削除
                        text = re.sub("\n$", "", text)
                        # 読み込んだ行が.jpgか.pngで終わる場合
                        if re.match("^.+\.[jJ][pP][eE]?[gG]$|^.+\.[pP][nN][gG]$", text):
                            # imgタグに置換
                            file.write('        <img id="' + escape_html(re.sub("\.[jJ][pP][eE]?[gG]$|\.[pP][nN][gG]$", "", text)) + '" src="' + escape_html(text) + '" />\n')
                        # 読み込んだ行が空行の場合
                        elif text == "":
                            # <p>全角空白</p>を出力
                            file.write('        <p>　</p>\n')
                        # 読み込んだ行が普通のテキストの場合
                        else:
                            # HTMLエスケープ
                            # HTMLエスケープは'を&#39;に置換
                            text = escape_html(text)
                            # 漢字(ひらがなかカタカナ)をルビに置換
                            text = re.sub("([一-鿋々]+)\(([ぁ-ゖァ-ヺー]+)\)", "<ruby>\\1<rt>\\2</rt></ruby>", text)
                            # ｜るび対象《ルビ》をルビに置換
                            text = re.sub("｜([^《]+)《([^》]+)》", "<ruby>\\1<rt>\\2</rt></ruby>", text)
                            # 漢字《ひらがなかカタカナ》をルビに置換
                            text = re.sub("([一-鿋々]+)《([ぁ-ゖァ-ヺー]+)》", "<ruby>\\1<rt>\\2</rt></ruby>", text)
                            file.write('        <p>' + text + '</p>\n')
                file.write('    </body>\n')
                file.write('</html>\n')

        # .epubファイルを作成
        # zip圧縮
        with zipfile.ZipFile(os.path.join(out_dir, title + ".epub"), 'w', zipfile.ZIP_STORED) as zip_file:
            zip_file.write(os.path.join(temp_dir.name, "mimetype"), "mimetype")
            zip_file.write(os.path.join(meta_inf_dir, "container.xml"), "META-INF/container.xml")
            zip_file.write(os.path.join(temp_dir.name, "opf.opf"), "opf.opf")
            zip_file.write(os.path.join(temp_dir.name, "common.css"), "common.css")
            zip_file.write(os.path.join(temp_dir.name, "nav.xhtml"), "nav.xhtml")
            # 画像ファイルの数だけ、くり返し
            for image_file_name in image_file_name_list:
                zip_file.write(os.path.join(temp_dir.name, image_file_name), image_file_name)
            # .txtファイルの数だけ、くり返し
            for number in range(1, len(text_file_name_list) + 1):
                zip_file.write(os.path.join(temp_dir.name, "index" + str(number) + ".xhtml"), "index" + str(number) + ".xhtml")

        # 一時ディレクトリを削除
        temp_dir.cleanup()

        message_string_var.set("電子書籍(ePub)の作成が完了しました。")

    except Exception as exception:
        messagebox.showerror(exception.__class__.__name__, str(exception))
        raise

make_epub_button = tkinter.Button(root, text="電子書籍(ePub)作成", command=make_epub)
make_epub_button.place(x=10, y=180)

root.mainloop()
