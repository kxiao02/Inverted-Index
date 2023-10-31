# Inverted-Index
This repository contains a Java implementation of an inverted index, a crucial data structure used in information retrieval systems to enable efficient full-text searches.

Overview
An inverted index is a mapping from terms to their locations in a set of documents. It allows for fast retrieval of documents that contain specific terms, making it a backbone for search engines and other applications that require quick text searches.

Components
The project is divided into several Java classes, each serving a specific purpose in the construction and utilization of the inverted index:

1. InvertedIndexBuilder
This class is responsible for building the inverted index. It processes the input documents, tokenizes the text, and updates the index with the terms and their corresponding locations.

2. Lexicon
The Lexicon class manages the terms in the inverted index, providing functionalities to add new terms and retrieve existing ones.

3. PageTable
This class represents the page table, keeping track of the documents and their metadata.

4. PostingBuilder
The PostingBuilder class is used for building the posting lists, which contain the document IDs and positions for each term in the index.

5. Run
This is the main class that orchestrates the building and querying of the inverted index. It initializes the necessary components and provides the user interface for interacting with the index.

6. Timer
A utility class for measuring the execution time of different parts of the program, helping in performance analysis.

7. Util
A utility class providing common functionalities used across the project.

License
This project is licensed under the MIT License - see the LICENSE.md file for details.
