import configparser
import logging


class ConfigManager:
    __config = configparser.ConfigParser()
    __logger = logging.getLogger(__name__)

    def __init__(self, config_file):
        #self.__config.read('config.ini')
        try:
            self.__config.read(config_file)
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_base_dir(self):
        try:
            return self.__config['default']['path']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_base_url(self):
        try:
            return self.__config['default']['url']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_db_file(self):
        try:
            return self.__config['database']['path']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_hash_size(self):
        try:
            return self.__config['repo']['id_length']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_deploy_base_path(self):
        try:
            return self.__config['default']['deploy_path']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_deploy_append_path(self):
        try:
            return self.__config['default']['deploy_path_append']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise

    def get_client_secret(self):
        try:
            return self.__config['default']['client_secret']
        except configparser.Error:
            self.__logger.error('Unable to parse config file.')
            raise