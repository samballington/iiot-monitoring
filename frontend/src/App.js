import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import axios from 'axios';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import './App.css';

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

function App() {
  const [sensorData, setSensorData] = useState(null);
  const [dataHistory, setDataHistory] = useState({
    timestamps: [],
    values: []
  });

  // Fetch sensor data every 2 seconds
  useEffect(() => {
    const fetchData = async () => {
      try {
        // Use the publicly accessible backend API endpoint
        console.log('Fetching sensor data from backend...');
        const response = await axios.get('http://localhost:8081/api/latest/sensorA');
        console.log('API Response:', response.data);
        
        if (response.data) {
          setSensorData(response.data);
          
          // Update history (keeping last 30 data points)
          setDataHistory(prev => {
            // Convert timestamp to readable format
            const timestamp = new Date(response.data.timestamp).toLocaleTimeString();
            
            // Add new data point to history arrays
            const newTimestamps = [...prev.timestamps, timestamp];
            const newValues = [...prev.values, response.data.value];
            
            // Keep only last 30 data points
            if (newTimestamps.length > 30) {
              newTimestamps.shift();
              newValues.shift();
            }
            
            return {
              timestamps: newTimestamps,
              values: newValues
            };
          });
        }
      } catch (error) {
        console.error('Error fetching sensor data:', error);
      }
    };

    fetchData(); // Initial fetch
    const interval = setInterval(fetchData, 2000);
    
    // Clean up interval on component unmount
    return () => clearInterval(interval);
  }, []);

  // Prepare chart data
  const chartData = {
    labels: dataHistory.timestamps,
    datasets: [
      {
        label: 'Sensor A Values',
        data: dataHistory.values,
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.5)',
        tension: 0.3,
      },
    ],
  };

  // Chart options
  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: 'Sensor Data Over Time',
      },
    },
    scales: {
      y: {
        beginAtZero: false,
      },
    },
  };

  // Format timestamp for display
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  return (
    <div className="App">
      <div className="dashboard-header">
        <h1 className="dashboard-title">IIoT Monitoring Dashboard</h1>
        <h3 className="dashboard-subtitle">Real-time sensor data visualization</h3>
      </div>

      <div className="sensor-card">
        <div className="sensor-header">
          <h2 className="sensor-title">Sensor A</h2>
          {sensorData && (
            <div className="sensor-value">{sensorData.value.toFixed(2)}</div>
          )}
        </div>
        
        {sensorData && (
          <div className="sensor-timestamp">
            Last updated: {formatTimestamp(sensorData.timestamp)}
          </div>
        )}
        
        <div className="chart-container">
          {dataHistory.sensorA.length > 0 && (
            <Line options={chartOptions} data={chartData} />
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
