import os
import shutil
import random


SOURCE_DIR = "data"

DEST_DIR = "splitted_data"
TRAIN_DIR = os.path.join(DEST_DIR, "train")
VAL_DIR = os.path.join(DEST_DIR, "val")
TEST_DIR = os.path.join(DEST_DIR, "test")

for subset in [TRAIN_DIR, VAL_DIR, TEST_DIR]:
    os.makedirs(subset, exist_ok=True)

for category in os.listdir(SOURCE_DIR):
    category_path = os.path.join(SOURCE_DIR, category)

    files = os.listdir(category_path)
    random.shuffle(files)

    total_files = len(files)
    train_size = int(total_files * 0.7)
    val_size = int(total_files * 0.15)
    test_size = total_files - train_size - val_size

    train_files = files[:train_size]
    val_files = files[train_size:train_size + val_size]
    test_files = files[train_size + val_size:]

    for subset, subset_files in zip([TRAIN_DIR, VAL_DIR, TEST_DIR], [train_files, val_files, test_files]):
        category_dest = os.path.join(subset, category)
        os.makedirs(category_dest, exist_ok=True)

        for file in subset_files:
            shutil.copy(os.path.join(category_path, file), os.path.join(category_dest, file))