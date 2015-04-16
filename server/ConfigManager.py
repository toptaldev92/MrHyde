import configparser
import logging


class ConfigManager:
    __config = configparser.ConfigParser()
    __logger = logging.getLogger(__name__)

    def __init__(self):
        self.__config.read('config.ini')

    def get_base_dir(self):
        try:
            return self.__config['default']['path']
        except KeyError:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_base_url(self):
        try:
            return self.__config['default']['url']
        except KeyError:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_db_file(self):
        try:
            return self.__config['database']['path']
        except KeyError:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_hash_size(self):
        try:
            return self.__config['repo']['id_length']
        except KeyError:
            self.__logger.error('Unable to parse config file.')
            raise
