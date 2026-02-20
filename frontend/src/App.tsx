import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Activity, BarChart3, Settings, Zap } from 'lucide-react';
import './App.css';
import Dashboard from './components/Dashboard';
import TaskManager from './components/TaskManager';
import RunMonitor from './components/RunMonitor';
import Settings from './components/Settings';

function App() {
  return (
    <Router>
      <div className="App">
        <nav className="sidebar">
          <div className="sidebar-header">
            <div className="logo">
              <Zap className="logo-icon" />
              <span className="logo-text">Agentic AI</span>
            </div>
          </div>
          
          <ul className="nav-menu">
            <li>
              <Link to="/" className="nav-link">
                <BarChart3 size={20} />
                <span>Dashboard</span>
              </Link>
            </li>
            <li>
              <Link to="/tasks" className="nav-link">
                <Activity size={20} />
                <span>Tasks</span>
              </Link>
            </li>
            <li>
              <Link to="/runs" className="nav-link">
                <Zap size={20} />
                <span>Runs</span>
              </Link>
            </li>
            <li>
              <Link to="/settings" className="nav-link">
                <Settings size={20} />
                <span>Settings</span>
              </Link>
            </li>
          </ul>
        </nav>

        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/tasks" element={<TaskManager />} />
            <Route path="/runs" element={<RunMonitor />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
