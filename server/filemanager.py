from os import makedirs, listdir
from os.path import isfile, isdir, join
import logging
import base64
from binascii import Error as Base64Error
from sqlite3 import Error as SQLError

from bottle import template

import dbhandler

logger = logging.getLogger(__name__)


class FileManager:
    __db = None
    __cm = None

    def __init__(self, cm):
        self.__cm = cm

    def cm(self):
        return self.__cm

    def create_diff_file(self, id, diff):
        try:
            file_name = ''.join(['/tmp/', id, '_patch.diff'])
            file_out = open(file_name, 'w')
        except IOError:
            logger.error('Unable to create diff file.')
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
            database = dbhandler.DbHandler(self.cm().get_db_file())
            repo_path = database.list('repo', 'path', "id='%s'" % repo_id)[0]
            abs_path = '/'.join([repo_path, ''.join(file_path), file_name])
            if isfile(abs_path):
                return True
            else:
                return False
        except OSError as exception:
            logger.error(exception.strerror)
            raise
        except SQLError as exception:
            logger.error(exception.__str__())
            raise

    def deploy_error_page(self, deploy_path, error_type, error_msg):
        if not isdir(deploy_path):
            makedirs(deploy_path, 0o755, True)
        index_file_path = join(deploy_path, 'index.html')
        index_file = open(index_file_path, 'w')
        index_file.write(template('list_view', rows=[error_msg], header=error_type))
        index_file.close()

    def list_directory(self, path):
        try:
            repo_path = ''.join([self.__cm.get_base_dir(), '/', path])
            file_list = [f for f in listdir(repo_path)]
            return file_list
        except OSError as exception:
            logger.error(exception.strerror)
            raise
        except SQLError as exception:
            logger.error(exception.__str__())
            raise

    def dispatch_static_files(self, deploy_path, files):
        for file in files:
            raw_path = file['path'].split('/')
            raw_data = file['data']
            file = raw_path[-1]
            data = None
            try:
                data = base64.standard_b64decode(raw_data)
            except Base64Error as exception:
                logger.warning(exception.__str__())
            rel_path = '/'.join(raw_path[0:-1])
            abs_path = '/'.join([deploy_path, rel_path])
            makedirs(abs_path, 0o755, True)
            filepath = '/'.join([abs_path, file])
            with open(filepath, 'wb') as out:
                if data is not None:
                    out.write(data)
