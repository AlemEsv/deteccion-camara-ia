const net = require('net');
const fs = require('fs');
const path = require('path');

const PORT = 9000;
const HOST = 'localhost';

function uploadImage(imagePath, label) {
    return new Promise((resolve, reject) => {
        const client = new net.Socket();
        const imageBuffer = fs.readFileSync(imagePath);
        const filename = path.basename(imagePath);
        const filesize = imageBuffer.length;

        const command = `UPLOAD:${label}:${filename}:${filesize}\n`;

        client.connect(PORT, HOST, () => {
            console.log(`Conectado al servidor ${HOST}:${PORT}`);
            console.log(`Subiendo imagen: ${filename} (${filesize} bytes) como ${label}`);

            client.write(command);

            client.write(imageBuffer);
        });

        let response = '';

        client.on('data', (data) => {
            response += data.toString();
            console.log('Respuesta:', data.toString().trim());

            if (response.includes('UPLOAD_SUCCESS') || response.includes('ERROR')) {
                client.end();
                resolve(response);
            }
        });

        client.on('error', (error) => {
            console.error('Error:', error.message);
            reject(error);
        });

        client.on('close', () => {
            console.log('Conexión cerrada');
        });
    });
}

function startTraining(epochs = null, batch = null) {
    return new Promise((resolve, reject) => {
        const client = new net.Socket();

        client.connect(PORT, HOST, () => {
            console.log(`Conectado al servidor ${HOST}:${PORT}`);
            
            let command = 'START_TRAIN';
            if (epochs !== null && batch !== null) {
                command += `:${epochs}:${batch}`;
                console.log(`Iniciando entrenamiento... (epochs: ${epochs}, batch: ${batch})`);
            } else {
                console.log('Iniciando entrenamiento... (usando valores por defecto)');
            }
            command += '\n';

            client.write(command);
        });

        let response = '';

        client.on('data', (data) => {
            const output = data.toString();
            response += output;
            
            process.stdout.write(output);

            if (output.includes('TRAINING_COMPLETE') || output.includes('ERROR')) {
                client.end();
                resolve(response);
            }
        });

        client.on('error', (error) => {
            console.error('Error:', error.message);
            reject(error);
        });

        client.on('close', () => {
            console.log('\nConexión cerrada');
        });
    });
}

async function main() {
    const args = process.argv.slice(2);

    if (args.length === 0) {
        console.log('Uso: node cliente-train.js <comando> [argumentos]');
        console.log('');
        console.log('Comandos:');
        console.log('  upload <ruta_imagen> <label>              - Subir una imagen');
        console.log('  train [epochs] [batch]                    - Iniciar entrenamiento');
        console.log('');
        console.log('Ejemplos:');
        console.log('  node cliente-train.js upload ../modelo-ia/dataset/images/train/img_1.jpg Perro');
        console.log('  node cliente-train.js train                - Usa valores por defecto');
        console.log('  node cliente-train.js train 20 8           - epochs=20, batch=8');
        process.exit(1);
    }

    const command = args[0];

    try {
        if (command === 'upload') {
            if (args.length < 3) {
                console.error('Error: Se requiere ruta de imagen y label');
                process.exit(1);
            }
            const imagePath = args[1];
            const label = args[2];

            if (!fs.existsSync(imagePath)) {
                console.error(`Error: Archivo no encontrado: ${imagePath}`);
                process.exit(1);
            }

            await uploadImage(imagePath, label);
        } else if (command === 'train') {
            const epochs = args.length > 1 ? parseInt(args[1], 10) : null;
            const batch = args.length > 2 ? parseInt(args[2], 10) : null;
            
            if (epochs !== null && isNaN(epochs)) {
                console.error('Error: epochs debe ser un número');
                process.exit(1);
            }
            if (batch !== null && isNaN(batch)) {
                console.error('Error: batch debe ser un número');
                process.exit(1);
            }
            
            await startTraining(epochs, batch);
        } else {
            console.error(`Error: Comando desconocido: ${command}`);
            process.exit(1);
        }
    } catch (error) {
        console.error('Error:', error.message);
        process.exit(1);
    }
}

if (require.main === module) {
    main();
}

module.exports = { uploadImage, startTraining };

