# ChemAxon - Interview assignment: Algorithms in Java
Please improve the existing code and make sure that the unit tests pass.

#### Tree traversal problem
The `TreeVertex` class implements traversal using depth and breadth first algorithms. 
* Finish unit tests to confirm proper functionality of the traversal algorithms.
* Modify `TreeVertex` class to support concurrency.

#### Cache problem
Create a class with a new proper cache implementation. The unit tests read from the cache instance and verify the number of read elements from DAO and expected cache hits. The cache implementation shall support concurrent access, however the DAO component does not. 

You shall also be able to explain (during interview):
* What is wrong with the `WrongCacheLogic` class implementation?
* Proper replacement policy: what kinds exist, which would you recommend and why?
* How would caching work in a distributed environment?

### Implementation hints:
* The assignment shall be sent as patch against `master` branch.
For example with `git format-patch master --stdout > ~/tmp/solution.patch`. Please do not publish a solution on
forked Github repository, since that could spoil testing of other candidates.
* The code must be possible to compile and unit tests must pass.
* Code shall be clean, following coding standards, documented.
* The commit log messages shall be short and self explaining.
* Commits shall be small, focusing on one area at a time.


### Evaluation:
The Git patch will be appliled and the application will be tested by running maven goal `test`. Evaluated will be (at no particular order):
* Way of solving the problem
* Following coding conventions and standards
* Commit style, code documentation


