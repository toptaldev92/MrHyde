import time
from os.path import join
import random
import string
from os.path import isdir

import DbHandler


class RepoUtils:
    __db = None

    def __init__(self):
        from main import cm
        self.__db = DbHandler.DbHandler(cm.get_db_file())

    def db(self):
        return self.__db

    def update_timestamp(self, id):
        self.db().updateData('repo', "id = '%s'" % id, 'last_used=%s' % int(time.time()))

    def get_expiration_date(self, id):
        # TODO bad hack!
        try:
            last_used = self.db().list('repo', 'last_used', "id='%s'" % id)[0]
            return last_used + (24 * 3600)
        except Exception as e:
            print(e)
            return time.time() + (24*3600)

    def generateId(self, length=16, chars=string.ascii_lowercase+string.digits):
            return ''.join(random.SystemRandom().choice(chars) for _ in range(length))

    def repository_exists(self, id):
        from main import cm
        if isdir(join(cm.get_base_dir(), id)):
            return True
        else:
            return False
