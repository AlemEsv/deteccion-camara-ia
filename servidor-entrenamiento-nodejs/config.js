module.exports = {
    PORT: 9000,
    PYTHON_MODULE_PATH: '../servidor-entrenamiento',
    DATASET_TRAIN_PATH: '../servidor-entrenamiento/dataset/images/train',
    DATASET_VAL_PATH: '../servidor-entrenamiento/dataset/images/val',
    LABELS_TRAIN_PATH: '../servidor-entrenamiento/dataset/labels/train',
    LABELS_VAL_PATH: '../servidor-entrenamiento/dataset/labels/val',
    TRAIN_SCRIPT: '../servidor-entrenamiento/src/train.py',
    DEFAULT_EPOCHS: 50,
    DEFAULT_BATCH: 16
  };