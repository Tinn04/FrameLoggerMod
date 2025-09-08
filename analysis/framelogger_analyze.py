import argparse, csv, math
import pandas as pd
import matplotlib
import os

def menu():
    print("What would you like to do?")
    print("1: Analyze frame logs")
    print("2: Read previous logs")
    print("3: Exit")
    userInput = input("")
    return userInput

def displayFiles(directoryName: str):
    # This gets the name of the current directory
    # Then go into the desired directory
    scriptDir = os.path.dirname(os.path.abspath(__file__))
    targetDir = os.path.join(scriptDir,  directoryName)

    # Check to see if targetDir actually exists in the folder
    # If not, print an error message and return to main menu
    if not os.path.isdir(targetDir):
        print(f"Directory '{directoryName}' not found.")
        menu()
    
    files = os.listdir(targetDir)

    # If the list is empty then print statement
    if not files:
        print(f"No files found in '{directoryName}'.")
        menu()
    
    for i, filename in enumerate(files, start = 1):
        print(f"{i}. {filename}")
    
    return(files)

def analyze():
    userInput = input("Choose CSV file to analyze: ")
    files = displayFiles("framelog")

    csvFile = files[userInput + 1]

    