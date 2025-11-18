const fs = require('fs').promises;
const path = require('path');
const config = require('../../config');

async function handleUpload(command, socket) {
    try {
        const { label, filename, imageData } = command.params;

        if (!label || !filename || !imageData) {
            socket.write('ERROR: Datos incompletos\n');
            return;
        }

        if (imageData.length === 0) {
            socket.write('ERROR: Imagen vacía\n');
            return;
        }

        const result = await saveImageWithLabel(
            imageData,
            filename,
            label,
            'train'
        );

        socket.write(`UPLOAD_SUCCESS:${filename}\n`);
        
        console.log(`Imagen guardada: ${result.imagePath}`);
        console.log(`Etiqueta creada: ${result.labelPath}`);

    } catch (error) {
        console.error('Error en handleUpload:', error);
        socket.write(`ERROR: ${error.message}\n`);
    }
}

async function saveImageToDataset(imageData, filename, split = 'train') {
    const datasetPath = split === 'train' 
        ? config.DATASET_TRAIN_PATH 
        : config.DATASET_VAL_PATH;
    
    const fullPath = path.resolve(__dirname, '../../', datasetPath, filename);
    
    await fs.mkdir(path.dirname(fullPath), { recursive: true });
    await fs.writeFile(fullPath, imageData);
    
    return fullPath;
}

async function createYoloLabel(filename, label, split = 'train', bbox = null) {
    const classMap = {
        'Perro': 0,
        'Gato': 1,
        'Persona': 2
    };

    const classId = classMap[label];
    if (classId === undefined) {
        throw new Error(`Etiqueta desconocida: ${label}`);
    }

    const labelsPath = split === 'train'
        ? config.LABELS_TRAIN_PATH
        : config.LABELS_VAL_PATH;

    const labelFilename = filename.replace(/\.(jpg|jpeg|png)$/i, '.txt');
    const fullPath = path.resolve(__dirname, '../../', labelsPath, labelFilename);

    await fs.mkdir(path.dirname(fullPath), { recursive: true });

    const labelContent = bbox
        ? `${classId} ${bbox[0]} ${bbox[1]} ${bbox[2]} ${bbox[3]}\n` 
        : `${classId} 0.5 0.5 0.5 0.5`;

    await fs.writeFile(fullPath, labelContent, 'utf-8');
    
    return fullPath;
}

async function saveImageWithLabel(imageData, filename, label, split = 'train', bbox) {
    const imagePath = await saveImageToDataset(imageData, filename, split);
    const labelPath = await createYoloLabel(filename, label, split, bbox);
    
    return {
        imagePath,
        labelPath
    };
}

module.exports = {
    handleUpload
};