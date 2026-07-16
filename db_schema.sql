CREATE TABLE `utenti` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nome` varchar(50) NOT NULL,
  `cognome` varchar(50) NOT NULL,
  `foto` varchar(255) DEFAULT 'defaultProPic.png',
  `admin` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_UNIQUE` (`username`)
);

CREATE TABLE `progetti` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nomeProgetto` varchar(100) NOT NULL,
  `durata` int NOT NULL,
  `idResponsabile` int NOT NULL,
  `stato` enum('CREATO','ASSEGNATO','CONCLUSO') NOT NULL DEFAULT 'CREATO',
  `idCreatore` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `progettiFK1_idx` (`idResponsabile`),
  KEY `progettiFK2_idx` (`idCreatore`),
  CONSTRAINT `progettiFK1` FOREIGN KEY (`idResponsabile`) REFERENCES `utenti` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `progettiFK2` FOREIGN KEY (`idCreatore`) REFERENCES `utenti` (`id`) ON UPDATE CASCADE
);

CREATE TABLE `work_packages` (
  `idWP` int NOT NULL AUTO_INCREMENT,
  `idProgetto` int NOT NULL,
  `numeroOrdine` int NOT NULL,
  `titolo` varchar(50) NOT NULL,
  `meseInizio` int NOT NULL,
  `meseFine` int NOT NULL,
  PRIMARY KEY (`idWP`),
  KEY `wpfk1_idx` (`idProgetto`),
  CONSTRAINT `wpfk1` FOREIGN KEY (`idProgetto`) REFERENCES `progetti` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `task` (
  `idTask` int NOT NULL AUTO_INCREMENT,
  `idWp` int NOT NULL,
  `numeroOrdine` int NOT NULL,
  `titolo` varchar(50) NOT NULL,
  `descrizione` text,
  PRIMARY KEY (`idTask`),
  KEY `taskfk1_idx` (`idWp`),
  CONSTRAINT `taskfk1` FOREIGN KEY (`idWp`) REFERENCES `work_packages` (`idWP`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `ore_lavorate` (
  `idTask` int NOT NULL,
  `mese` int NOT NULL,
  `idCollaboratore` int NOT NULL,
  `ore` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`idTask`,`mese`,`idCollaboratore`),
  KEY `oreLfk2_idx` (`idCollaboratore`),
  CONSTRAINT `oreLfk1` FOREIGN KEY (`idTask`) REFERENCES `task` (`idTask`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `oreLfk2` FOREIGN KEY (`idCollaboratore`) REFERENCES `utenti` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `ore_previste` (
  `idTask` int NOT NULL,
  `mese` int NOT NULL,
  `ore` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`idTask`,`mese`),
  CONSTRAINT `orePfk1` FOREIGN KEY (`idTask`) REFERENCES `task` (`idTask`) ON DELETE CASCADE ON UPDATE CASCADE
);