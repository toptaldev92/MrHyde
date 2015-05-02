import logging
import subprocess
import re
import os

OWN_DIR = os.path.dirname(os.path.realpath(__file__))

class JekyllManager:
    __regex = None
    __error_list = None

    __logger = logging.getLogger(__name__)

    def __init__(self):
        self.__regex = re.compile("Error.*\.")

    def log(self):
        return self.__logger

    def build(self, build_path, deploy_path):
        cmd = ['jekyll', 'build', '--source', build_path, '--destination', deploy_path, '--config', build_path+'/_config.yml,'+OWN_DIR+'/keep_files.yml']
        with open(deploy_path+'/input.txt', 'w') as outfile:
            status_code = subprocess.call(cmd, stdout=outfile)
        with open(deploy_path+'/statuscode.txt', 'w') as outfile:
            outfile.write(str(status_code))

    def get_errors(self):
        return self.__error_list
