import logging
import subprocess
import re


class JekyllManager:
    __regex = None
    __error_list = None

    __logger = logging.getLogger(__name__)

    def __init__(self):
        self.__regex = re.compile("Error.*\.")

    def log(self):
        return self.__logger

    def build(self, build_path, deploy_path):
        # TODO perhaps we should do this async (big pages?)
        cmd = ['jekyll', 'build', '--source', build_path, '--destination', deploy_path]
        status_code = 9
        with open(deploy_path+'input.txt') as outfile:
            status_code = subprocess.call(cmd, stdout=outfile)
        with open(deploy_path+'statuscode.txt') as outfile:
            outfile.write(str(status_code))

    def get_errors(self):
        return self.__error_list
