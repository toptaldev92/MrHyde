import time
from os.path import join
import random
import string
from os.path import isdir
from sqlite3 import Error as SQLError
import logging

import dbhandler

logger = logging.getLogger(__name__)


class RepoUtils:
    __cm = None

    def __init__(self, cm):
        self.__cm = cm

    def cm(self):
        return self.__cm

    def update_timestamp(self, id):
        try:
            database = dbhandler.DbHandler(self.cm().get_db_file())
            database.updateData('repo', "id = '%s'" % id, 'last_used=%s' % int(time.time()))
        except SQLError as exception:
            logger.error(exception.__str__())
            raise

    def get_expiration_date(self, id):
        try:
            database = dbhandler.DbHandler(self.cm().get_db_file())
            last_used = database.list('repo', 'last_used', "id='%s'" % id)[0]
            return last_used + (24 * 3600)
        except SQLError as exception:
            logger.error(exception.__str__())
            raise

    def generateId(self, length=16, chars=string.ascii_lowercase+string.digits):
            return ''.join(random.SystemRandom().choice(chars) for _ in range(length))

    def repository_exists(self, id):
        if isdir(join(self.cm().get_base_dir(), id)):
            return True
        else:
            return False
