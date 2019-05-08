# Recursive-Descent-Parser
Implemented and compared the efficiency of recursive descent parsing in Java, Dart and Python. The input to each of the implmentation is a program implemented in WebAssembly (more info about WebAssembly here https://webassembly.org/). There are 4 main modules in each implementation. A breakdown of the modules and what they do breifly is explained below:

ST:
 - Creates a global data structure that stores any new symbols. Symbols can be variables, function names, etc.
 
SC:
- Scans/parsers the input and validates if it is syntactically corerct

P0:
- Handles the logic of the source input and determines which code is needed to be generated. 

CGwat:
- This module is responsible for code generation and storeing it into a global data structure. 


The Python implementataion is developed by Professor Emil Sekerinski from McMaster University. 
