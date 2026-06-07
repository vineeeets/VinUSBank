-- Phase 1 Databases
CREATE DATABASE IF NOT EXISTS `vinusbank_auth`;
CREATE DATABASE IF NOT EXISTS `vinusbank_customer`;

-- Phase 2 Databases
CREATE DATABASE IF NOT EXISTS `vinusbank_account`;
CREATE DATABASE IF NOT EXISTS `vinusbank_transaction`;
CREATE DATABASE IF NOT EXISTS `vinusbank_loan`;
CREATE DATABASE IF NOT EXISTS `vinusbank_card`;

-- Phase 3 Databases
CREATE DATABASE IF NOT EXISTS `vinusbank_compliance`;
CREATE DATABASE IF NOT EXISTS `vinusbank_notification`;

-- Grants
GRANT ALL PRIVILEGES ON `vinusbank_auth`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_customer`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_account`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_transaction`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_loan`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_card`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_compliance`.* TO 'vinusbank_user'@'%';
GRANT ALL PRIVILEGES ON `vinusbank_notification`.* TO 'vinusbank_user'@'%';

FLUSH PRIVILEGES;

