class ProtocolParser {
    constructor() {
        this.buffer = Buffer.alloc(0);
        this.expectedDataSize = null;
        this.currentCommand = null;
        this.commandParams = null;
    }

    processData(data) {
        this.buffer = Buffer.concat([this.buffer, data]);

        if (this.expectedDataSize !== null) {
            return this.processBinaryData();
        }

        const newlineIndex = this.buffer.indexOf('\n');
        if (newlineIndex === -1) {
            return null;
        }

        const commandLine = this.buffer.slice(0, newlineIndex).toString('utf-8');
        this.buffer = this.buffer.slice(newlineIndex + 1);

        const commandResult = this.parseCommand(commandLine);

        if (commandResult.type === 'ERROR' || commandResult.type === 'START_TRAIN') {
            return commandResult;
        }

        if (commandResult.type === 'UPLOAD'){
            return this.processBinaryData();
        }

        return { type: 'ERROR', message: 'Estado de parser desconocido' }; 
    }

    parseCommand(commandLine) {
        const trimmed = commandLine.trim();

        if (trimmed.startsWith('START_TRAIN')) {
            if (trimmed === 'START_TRAIN') {
                return {
                    type: 'START_TRAIN',
                    params: {}
                };
            }
            
            const parts = trimmed.substring(11).split(':');
            if (parts.length === 2) {
                const epochs = parseInt(parts[0], 10);
                const batch = parseInt(parts[1], 10);
                
                if (isNaN(epochs) || isNaN(batch) || epochs <= 0 || batch <= 0) {
                    return { type: 'ERROR', message: 'Parámetros START_TRAIN inválidos' };
                }
                
                return {
                    type: 'START_TRAIN',
                    params: { epochs, batch }
                };
            }
            
            return { type: 'ERROR', message: 'Formato START_TRAIN inválido. Use: START_TRAIN o START_TRAIN:epochs:batch' };
        }

        if (trimmed.startsWith('UPLOAD:')) {
            const parts = trimmed.substring(7).split(':');
            if (parts.length !== 3) {
                return { type: 'ERROR', message: 'Formato UPLOAD inválido' };
            }

            const [label, filename, filesizeStr] = parts;
            const filesize = parseInt(filesizeStr, 10);

            if (isNaN(filesize) || filesize <= 0) {
                return { type: 'ERROR', message: 'Tamaño de archivo inválido' };
            }

            this.currentCommand = 'UPLOAD';
            this.commandParams = { label, filename, filesize };
            this.expectedDataSize = filesize;

            return {
                type: 'UPLOAD',
                params: { label, filename, filesize }
            };
        }

        return { type: 'ERROR', message: 'Comando desconocido' };
    }

    processBinaryData() {
        if (this.buffer.length < this.expectedDataSize) {
            return null;
        }

        const imageData = this.buffer.slice(0, this.expectedDataSize);
        this.buffer = this.buffer.slice(this.expectedDataSize);

        const result = {
            type: 'UPLOAD',
            params: {
                ...this.commandParams,
                imageData: imageData
            }
        };

        this.expectedDataSize = null;
        this.currentCommand = null;
        this.commandParams = null;

        return result;
    }

    reset() {
        this.buffer = Buffer.alloc(0);
        this.expectedDataSize = null;
        this.currentCommand = null;
        this.commandParams = null;
    }
}

module.exports = ProtocolParser;