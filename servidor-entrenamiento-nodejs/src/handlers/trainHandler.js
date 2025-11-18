const { spawn } = require('child_process');
const path = require('path');
const config = require('../../config');

let isTraining = false;

async function handleTrain(command, socket) {

    if (isTraining) {
        socket.write('ERROR: Ya hay un entrenamiento en curso\n');
        return;
    }

    isTraining = true;

    try {
        const { epochs, batch } = command.params;

        const epochsValue = epochs || config.DEFAULT_EPOCHS;
        const batchValue = batch || config.DEFAULT_BATCH;
        console.log(`Iniciando entrenamiento... (epochs: ${epochsValue}, batch: ${batchValue})`);

        socket.write('TRAINING_STARTED\n');

        const onOutput = (output) => {
            process.stdout.write(output);
        };

        const onError = (error) => {
            process.stderr.write(error);
        };

        const result = await runTraining(
            epochs,
            batch,
            onOutput,
            onError
        );

    } catch (error) {
        console.error('Error en handleTrain:', error);
        socket.write(`ERROR: ${error.message}\n`);
    } finally {
        isTraining = false;
    }
}

async function runTraining(epochs = null, batch = null, onOutput = null, onError = null) {
    return new Promise((resolve, reject) => {
        const epochsValue = epochs || config.DEFAULT_EPOCHS;
        const batchValue = batch || config.DEFAULT_BATCH;

        const scriptPath = path.resolve(__dirname, '../../', config.TRAIN_SCRIPT);
        const scriptDir = path.dirname(scriptPath);

        const pythonProcess = spawn('python3', [
            scriptPath,
            epochsValue.toString(),
            batchValue.toString()
        ], {
            cwd: scriptDir,
            stdio: ['ignore', 'pipe', 'pipe']
        });

        let stdoutData = '';
        let stderrData = '';

        pythonProcess.stdout.on('data', (data) => {
            const output = data.toString();
            stdoutData += output;
            
            if (onOutput) {
                onOutput(output);
            }

            if (output.includes('TRAINING_COMPLETE')) {
                resolve('TRAINING_COMPLETE');
            }
        });

        pythonProcess.stderr.on('data', (data) => {
            const error = data.toString();
            stderrData += error;
            
            if (onError) {
                onError(error);
            }
        });

        pythonProcess.on('close', (code) => {
            if (code === 0) {
                if (stdoutData.includes('TRAINING_COMPLETE')) {
                    resolve('TRAINING_COMPLETE');
                } else {
                    resolve('COMPLETED');
                }
            } else {
                reject(new Error(`Proceso Python terminó con código ${code}\n${stderrData}`));
            }
        });

        pythonProcess.on('error', (error) => {
            reject(new Error(`Error al ejecutar Python: ${error.message}`));
        });
    });
}

module.exports = {
    handleTrain
};