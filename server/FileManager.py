from os import makedirs, listdir
from os.path import isfile
import errno
import logging

from sqlite3 import Error as SQLError

import DbHandler


class FileManager:
    __logger = None
    __db = None

    def __init__(self):
        from main import cm
        self.__logger = logging.getLogger(__name__)
        self.__db = DbHandler.DbHandler(cm.get_db_file())

    def log(self):
        return self.__logger

    def database(self):
        return self.__db

    def create_diff_file(self, id, diff):
        try:
            file_name = ''.join(['/tmp/', id, '_patch.diff'])
            file_out = open(file_name, 'w')
        except IOError:
            self.log().error('Unable to create diff file.')
            raise
        else:
            file_out.write(diff)
            file_out.close()
            return file_name

    def file_download(self, id, file_name):
        repo_id = id.split('/')[0]
        if id.split('/')[1:]:
            file_path = id.split('/')[1:][0]
        else:
            file_path = []

        try:
            repo_path = self.database().list('repo', 'path', "id='%s'" % repo_id)[0]
            abs_path = '/'.join([repo_path, ''.join(file_path), file_name])
            if isfile(abs_path):
                return True
            else:
                return False
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error('File' + file_path + ' not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to access file ' + file_path + '.')
                raise

    def setup_deployment(self, id):
        from main import cm
        try:
            deploy_path = ''.join([cm.get_deploy_base_path(), id, cm.get_deploy_append_path()])
            makedirs(deploy_path, 0o755, True)
            self.log().info('Created deploy path ' + deploy_path)
            return deploy_path
        except OSError as exception:
            if exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to create directory: ' + deploy_path)
                return None

    def list_directory(self, path):
        from main import cm
        try:
            repo_path = ''.join([cm.get_base_dir(), '/', path])
            file_list = [f for f in listdir(repo_path)]
            return file_list
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error("Repository " + repo_path + " doesn't exist.")
                raise
            elif exception.errno == errno.EPERM:
                self.log().error("Insufficient permissions to view " + repo_path + ".")
                raise
        except SQLError:
            self.log().error('Database error.')
            raise
