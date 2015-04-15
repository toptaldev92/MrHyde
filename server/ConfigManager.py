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
