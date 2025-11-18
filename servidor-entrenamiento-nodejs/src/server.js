const net = require('net');
const config = require('../config');
const ProtocolParser = require('./protocol/parser');
const { handleUpload } = require('./handlers/uploadHandler');
const { handleTrain } = require('./handlers/trainHandler');
const { Socket } = require('dgram');

class TrainingServer {
    constructor() {
        this.server = null;
        this.port = config.PORT;
    }

    start() {
        this.server = net.createServer((socket) => {
            console.log(`Cliente conectado: ${socket.remoteAddress}:${socket.remotePort}`);

            const parser = new ProtocolParser();

            socket.on('data', async (data) => {
                try {
                    const command = parser.processData(data);

                    if (!command) {
                        return;
                    }

                    if (command.type === 'ERROR') {
                        socket.write(`ERROR: ${command.message}\n`);
                        return;
                    }

                    switch (command.type) {
                        case 'UPLOAD':
                            await handleUpload(command, socket);
                            break;

                        case 'START_TRAIN':
                            await handleTrain(command, socket);
                            break;

                        default:
                            socket.write(`ERROR: Comando desconocido: ${command.type}\n`);
                    }

                } catch (error) {
                    console.error('Error procesando datos:', error);
                    socket.write(`ERROR: ${error.message}\n`);
                }
            });

            socket.on('end', () => {
                console.log(`Cliente desconectado: ${socket.remoteAddress}:${socket.remotePort}`);
                parser.reset();
            });

            socket.on('error', (error) => {
                console.error('Error en socket:', error);
            });
        });

        this.server.listen(this.port, () => {
            console.log(`Servidor de Entrenamiento escuchando en puerto ${this.port}`);
            console.log(`Esperando conexiones...`);
        });

        this.server.on('error', (error) => {
            if (error.code === 'EADDRINUSE') {
                console.error(`Error: El puerto ${this.port} ya está en uso`);
            } else {
                console.error('Error del servidor:', error);
            }
            process.exit(1);
        });
    }

    stop() {
        if (this.server) {
            this.server.close(() => {
                console.log('Servidor detenido');
            });
        }
    }
}

if (require.main === module) {
    const server = new TrainingServer();
    server.start();

    process.on('SIGINT', () => {
        console.log('\nCerrando servidor...');
        server.stop();
        process.exit(0);
    });

    process.on('SIGTERM', () => {
        console.log('\nCerrando servidor...');
        server.stop();
        process.exit(0);
    });
}

module.exports = TrainingServer;