cd tsp_drone_library
mvn clean 
mvn install
cd ..
cd tsp_drone_solver
mvn clean
mvn install
cp target/tsp_drone_solver-1.0-jar-with-dependencies.jar ../testing
