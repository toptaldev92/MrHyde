import configparser
import logging

logger = logging.getLogger(__name__)


class ConfigManager:
    __config = configparser.ConfigParser()

    def __init__(self, config_file):
        try:
            self.__config.read(config_file)
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_base_dir(self):
        try:
            return self.__config['default']['path']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_base_url(self):
        try:
            return self.__config['default']['url']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_db_file(self):
        try:
            return self.__config['database']['path']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_hash_size(self):
        try:
            return int(self.__config['repo']['id_length'])
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_deploy_base_path(self):
        try:
            return self.__config['default']['deploy_path']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_deploy_append_path(self):
        try:
            return self.__config['default']['deploy_path_append']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_client_secret(self):
        try:
            return self.__config['default']['client_secret']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise

    def get_cleanup_time(self):
        try:
            return self.__config['caretaker']['time_limit']
        except configparser.Error:
            logger.error('Unable to parse config file.')
            raise
