#!/bin/bash

# Run the Jupyter notebook and wait for it to complete
papermill ./chapter1.ipynb output.ipynb

echo "Notebook execution completed!"
