import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Brain, History, Sparkles, GitBranch } from 'lucide-react';
import './App.css';

// Import components with proper error handling
const OrchestrationEngine = React.lazy(() => import('./components/OrchestrationEngine'));
const ExecutionHistory = React.lazy(() => import('./components/ExecutionHistory'));

// TypeScript declaration for hot module replacement
declare const module: any;

// Add hot module replacement for development
if (process.env.NODE_ENV === 'development' && module.hot) {
  module.hot.accept();
}

function NavigationContent() {
  const location = useLocation();
  
  return (
    <nav className="modern-sidebar">
      <div className="sidebar-header">
        <div className="logo">
          <div className="logo-icon-wrapper">
            <Brain className="logo-icon" />
            <div className="logo-pulse"></div>
          </div>
          <div className="logo-text">
            <span className="brand-name">Agentic AI</span>
            <span className="brand-subtitle">Multi-Agent Orchestration</span>
          </div>
        </div>
      </div>
      
      <div className="nav-section">
        <div className="nav-section-title">ORCHESTRATION</div>
        <ul className="nav-menu">
          <li>
            <Link 
              to="/" 
              className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
            >
              <div className="nav-icon">
                <GitBranch size={22} />
              </div>
              <div className="nav-content">
                <span className="nav-title">Prompt Engine</span>
                <span className="nav-description">Real-time AI orchestration</span>
              </div>
              {location.pathname === '/' && <div className="nav-indicator" />}
            </Link>
          </li>
          <li>
            <Link 
              to="/history" 
              className={`nav-link ${location.pathname === '/history' ? 'active' : ''}`}
            >
              <div className="nav-icon">
                <History size={22} />
              </div>
              <div className="nav-content">
                <span className="nav-title">Execution History</span>
                <span className="nav-description">Past runs & artifacts</span>
              </div>
              {location.pathname === '/history' && <div className="nav-indicator" />}
            </Link>
          </li>
        </ul>
      </div>

      <div className="sidebar-footer">
        <div className="tech-stack">
          <div className="tech-item">
            <Sparkles size={16} />
            <span>AWS Bedrock</span>
          </div>
          <div className="tech-item">
            <Brain size={16} />
            <span>Multi-Agent AI</span>
          </div>
        </div>
      </div>
    </nav>
  );
}

function App() {
  return (
    <Router>
      <div className="App">
        <NavigationContent />
        
        <main className="main-content">
          <React.Suspense fallback={
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              height: '100vh',
              color: 'white'
            }}>
              Loading...
            </div>
          }>
            <Routes>
              <Route path="/" element={<OrchestrationEngine />} />
              <Route path="/history" element={<ExecutionHistory />} />
            </Routes>
          </React.Suspense>
        </main>
      </div>
    </Router>
  );
}

export default App;
