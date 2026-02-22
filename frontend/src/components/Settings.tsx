import React, { useState } from 'react';
import { Save, Server, Database, Key, Globe, Shield } from 'lucide-react';

const Settings: React.FC = () => {
  const [settings, setSettings] = useState({
    apiEndpoint: 'http://localhost:8080',
    refreshInterval: 5,
    maxRetries: 3,
    timeout: 30
  });
  
  const [saving, setSaving] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    
    // Simulate saving settings
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    setSaving(false);
    alert('Settings saved successfully!');
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1 className="dashboard-title">Settings</h1>
        <p className="dashboard-subtitle">
          Configure your agentic AI system preferences and connections
        </p>
      </div>

      <div className="content-grid">
        {/* API Configuration */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">API Configuration</h3>
          </div>
          <div className="card-content">
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">
                  <Server size={16} className="inline mr-2" />
                  API Endpoint
                </label>
                <input
                  type="url"
                  className="form-input"
                  value={settings.apiEndpoint}
                  onChange={(e) => setSettings({...settings, apiEndpoint: e.target.value})}
                />
              </div>
              
              <div className="form-group">
                <label className="form-label">
                  <Globe size={16} className="inline mr-2" />
                  Refresh Interval (seconds)
                </label>
                <input
                  type="number"
                  className="form-input"
                  value={settings.refreshInterval}
                  onChange={(e) => setSettings({...settings, refreshInterval: parseInt(e.target.value)})}
                  min="1"
                  max="60"
                />
              </div>
              
              <div className="form-group">
                <label className="form-label">
                  <Shield size={16} className="inline mr-2" />
                  Max Retries
                </label>
                <input
                  type="number"
                  className="form-input"
                  value={settings.maxRetries}
                  onChange={(e) => setSettings({...settings, maxRetries: parseInt(e.target.value)})}
                  min="1"
                  max="10"
                />
              </div>
              
              <div className="form-group">
                <label className="form-label">
                  <Key size={16} className="inline mr-2" />
                  Request Timeout (seconds)
                </label>
                <input
                  type="number"
                  className="form-input"
                  value={settings.timeout}
                  onChange={(e) => setSettings({...settings, timeout: parseInt(e.target.value)})}
                  min="5"
                  max="300"
                />
              </div>
              
              <button
                type="submit"
                className="btn btn-primary"
                disabled={saving}
              >
                <Save size={16} />
                {saving ? 'Saving...' : 'Save Settings'}
              </button>
            </form>
          </div>
        </div>

        {/* System Info */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">System Information</h3>
          </div>
          <div className="card-content">
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="font-medium">Frontend Version</span>
                <span className="text-gray-600">v1.0.0</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-medium">Backend API</span>
                <span className="text-green-600">Connected</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-medium">Database</span>
                <span className="text-green-600">PostgreSQL</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-medium">Environment</span>
                <span className="text-gray-600">Development</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* About Section */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">About Agentic AI System</h3>
        </div>
        <div className="card-content">
          <div className="text-gray-600 space-y-4">
            <p>
              This is a professional agentic AI system that uses multiple AI agents (Planner, Executor, Critic) 
              to process complex tasks through a structured workflow.
            </p>
            <p>
              Built with Spring Boot backend, React frontend, and deployed on AWS with proper IAM security.
            </p>
            <div className="mt-6 p-4 bg-blue-50 rounded-lg">
              <h4 className="font-semibold text-blue-900 mb-2">Key Features:</h4>
              <ul className="list-disc list-inside text-blue-800 space-y-1">
                <li>Multi-agent AI workflow (Plan → Execute → Critique)</li>
                <li>Real-time task monitoring and execution tracking</li>
                <li>Concurrent processing with race condition protection</li>
                <li>Professional dashboard with charts and analytics</li>
                <li>AWS cloud deployment with proper security</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;