module.exports = {
    PORT: 9000,
    PYTHON_MODULE_PATH: '../modelo-ia',
    DATASET_TRAIN_PATH: '../modelo-ia/dataset/images/train',
    DATASET_VAL_PATH: '../modelo-ia/dataset/images/val',
    LABELS_TRAIN_PATH: '../modelo-ia/dataset/labels/train',
    LABELS_VAL_PATH: '../modelo-ia/dataset/labels/val',
    TRAIN_SCRIPT: '../modelo-ia/src/train.py',
    DEFAULT_EPOCHS: 50,
    DEFAULT_BATCH: 16
  };