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
        try:
            process = subprocess.check_output(cmd)
            return True
        except subprocess.CalledProcessError as exception:
            process = exception.output
            self.__error_list = self.__regex.findall(process)
            return False

    def get_errors(self):
        return self.__error_list
