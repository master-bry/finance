# Finance Tracker - Startup Guide

## Quick Start

### Using the Custom Startup Scripts

#### Linux/macOS
```bash
./start.sh
```

#### Windows
```cmd
start.bat
```

## What the Startup Scripts Do

The custom startup scripts automatically:

1. **Check Environment**
   - Verify Java 17+ is installed
   - Check for Maven wrapper
   - Validate `.env` file exists

2. **Setup Configuration**
   - Create `.env` file from `.env.example` if missing
   - Load environment variables
   - Check if port 8080 is available

3. **Start Application**
   - Run `./mvnw spring-boot:run` (Linux/macOS)
   - Run `mvnw.cmd spring-boot:run` (Windows)

## Manual Setup (If Scripts Fail)

### 1. Environment Configuration
```bash
# Copy the example environment file
cp .env.example .env

# Edit with your MongoDB credentials
nano .env
```

### 2. Install Requirements
- **Java 17+** required
- **Maven** (included via wrapper)

### 3. Start Application
```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or using system Maven
mvn spring-boot:run
```

## Configuration

### MongoDB Setup
1. Go to [MongoDB Atlas](https://cloud.mongodb.com)
2. Create a free cluster
3. Create a database user
4. Get connection string from Connect → Drivers
5. Update `.env` file with your credentials

### Environment Variables (.env)
```env
# MongoDB Atlas Connection String
MONGODB_URI=mongodb+srv://your-username:your-password@finance-tracker.mongodb.net/finance-tracker?retryWrites=true&w=majority

# Database Name
MONGODB_DATABASE=finance-tracker

# Server Port (optional, defaults to 8080)
SERVER_PORT=8080
```

## Accessing the Application

Once started, access the application at:
- **URL**: http://localhost:8080
- **Default Port**: 8080

## Troubleshooting

### Port Already in Use
The scripts automatically detect and attempt to free port 8080. If this fails:
```bash
# Kill process on port 8080 (Linux/macOS)
sudo lsof -ti:8080 | xargs kill -9

# Or change port in .env
SERVER_PORT=8081
```

### Java Version Issues
```bash
# Check Java version
java -version

# Install Java 17 (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-17-jdk

# Install Java 17 (macOS with Homebrew)
brew install openjdk@17
```

### MongoDB Connection Issues
1. Verify MongoDB Atlas credentials in `.env`
2. Check network connectivity
3. Ensure IP address is whitelisted in MongoDB Atlas

## Development Mode

The application includes Spring Boot DevTools for hot reloading during development.

## Production Deployment

For production deployment:
1. Build the JAR file:
   ```bash
   ./mvnw clean package
   ```
2. Run the JAR:
   ```bash
   java -jar target/finance-1.0.0.jar
   ```

## Support

If you encounter issues:
1. Check the console output for error messages
2. Verify all requirements are met
3. Ensure MongoDB Atlas is properly configured
4. Check that the `.env` file contains correct credentials
