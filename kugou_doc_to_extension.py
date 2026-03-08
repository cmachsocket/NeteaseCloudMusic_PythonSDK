import json

with open("kugou_fun.json", "r", encoding="utf-8") as f:
    apis = json.load(f)

def path_to_func_name(path):
    # 去掉斜杠，分割单词
    parts = path.strip("/").split("/")
    func_name = "_".join(parts)
    return func_name


python_all_code = ""
dart_all_code = ""

def gender_python_code(api):
    global python_all_code
    doc_lines = api["doc"].split("\n")
    cleaned_lines = []
    for line in doc_lines:
        if not line.strip():
            continue
        # 去除 markdown 语法
        line = line.replace("**", "").replace("`", "").strip()
        # 去除行首冒号
        if line.startswith(":"):
            line = line[1:]
        # 加8个空格
        cleaned_lines.append("        " + line)
    docstring = "\n".join(cleaned_lines)

    code = f"""    def {path_to_func_name(api["path"])}(self, {', '.join(api["required"]) if api["required"] else ''}{', ' if api["required"] else ''}{', '.join([f'{p}=None' for p in api["optional"]]) if api["optional"] else ''}{', ' if api["optional"] else ''}cookie = "", env: KugouProcessEnv = None) -> Response:
        \'\'\'
{docstring}
        \'\'\'
        return self.request("{api['path']}", cookie, env{', ' if api["required"]+api["optional"] else ''}{', '.join([f'{p}={p}' for p in api["required"]+api["optional"]]) if api["required"]+api["optional"] else ''})
"""
    python_all_code += code

def gender_dart_code(api):
    global dart_all_code
    doc_lines = api["doc"].split("\n")
    cleaned_lines = []
    for line in doc_lines:
        # 去除 markdown 语法
        line = line.replace("**", "").replace("`", "").strip()
        # 去除行首冒号
        if line.startswith(":"):
            line = line[1:]
        # 加2个空格
        cleaned_lines.append("  ///" + line)
    docstring = "\n".join(cleaned_lines)


# {', '.join([f'{p}={p}' for p in api["required"]+api["optional"]]) if api["required"]+api["optional"] else ''}

    code = f"""{docstring}
  MusicResponse {path_to_func_name(api["path"])}({', '.join([f'String {p}' for p in api["required"]]) if api["required"] else ''}{', ' if api["required"] else ''}{{{', '.join([f'String? {p}' for p in api["optional"]]) if api["optional"] else ''}{', ' if api["optional"] else ''}Map<String, String> cookie = const {{}}, KugouProcessEnv? env}}){{
    return request("{api['path']}", cookie:cookie, env:env{', query: {' if api["required"]+api["optional"] else ''}{', '.join([f"'{p}':{p}" for p in api["required"]+api["optional"]]) if api["required"]+api["optional"] else ''}{'}' if api["required"]+api["optional"] else ''});
    }}
"""
    dart_all_code += code

for api in apis:  # 只打印前5个接口信息
    gender_python_code(api)
    gender_dart_code(api)


python_file = "kuGouMusicApiExtension.py"
dart_file = "kuGouMusicApiExtension.dart"

with open(python_file, "w", encoding="utf-8") as f:
    f.write("class Extension:\n")
    f.write(python_all_code)

with open(dart_file, "w", encoding="utf-8") as f:
    f.write("class Extension {\n")
    f.write(dart_all_code)
