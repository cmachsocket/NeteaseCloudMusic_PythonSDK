import json,os
# from .login.loginStatus import load_login_status
from MusicLibrary.neteaseCloudMusicApi import NeteaseCloudMusicApi, NcmProcessEnv

# user_data_file = os.path.join(os.path.dirname(__file__), "login", "loginStatus.json")

# cookie, userid = load_login_status()

# print("当前cookie:", cookie)

ncm = NeteaseCloudMusicApi(NcmProcessEnv())

# kugou.set_cookie(cookie)

# captcha_response = ncm.request("/captcha/sent", phone="")
# print(captcha_response)

response = ncm.request("/login/cellphone", phone="", captcha="")
print(response)