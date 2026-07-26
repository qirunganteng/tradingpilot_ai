import React, { useState, useEffect } from 'react';
import {
  FolderOpen,
  LineChart,
  MessageSquare,
  Calculator,
  Settings,
  Sparkles,
  ShieldCheck,
  RefreshCw,
  Play,
  Terminal,
  Copy,
  Check,
  Globe,
  Lock,
  ChevronRight,
  ChevronLeft,
  Search,
  Plus,
  GitBranch,
  CheckCircle2,
  FileCode,
  Sliders,
  Maximize2,
  Minimize2,
  Eye,
  EyeOff,
  Send,
  ArrowUpRight,
  ArrowDownRight,
  HelpCircle,
  Code2,
  Layers,
  Activity,
  Bookmark,
  GripVertical
} from 'lucide-react';

// --- Types ---
type ActivityTab = 'explorer' | 'chart' | 'aichat' | 'risk' | 'settings';
type PairSymbol = 'EUR/USD' | 'XAU/USD' | 'BTC/USD' | 'GBP/JPY';
type Timeframe = 'M5' | 'M15' | 'H1' | 'H4' | 'D1';

interface ChatMessage {
  id: string;
  sender: 'user' | 'gemini';
  text: string;
  timestamp: string;
  codeSnippet?: string;
}

export default function App() {
  // Navigation & Browser State
  const [activeActivityTab, setActiveActivityTab] = useState<ActivityTab>('chart');
  const [selectedPair, setSelectedPair] = useState<PairSymbol>('EUR/USD');
  const [selectedTimeframe, setSelectedTimeframe] = useState<Timeframe>('H1');
  const [showOverlays, setShowOverlays] = useState<boolean>(true);
  const [rightPanelTab, setRightPanelTab] = useState<'ai' | 'risk' | 'terminal'>('ai');

  // Resizable Panels State
  const [sidebarWidth, setSidebarWidth] = useState<number>(224);
  const [rightPanelWidth, setRightPanelWidth] = useState<number>(320);
  const [isDraggingLeft, setIsDraggingLeft] = useState<boolean>(false);
  const [isDraggingRight, setIsDraggingRight] = useState<boolean>(false);

  // Mouse move listener for resizable dividers
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isDraggingLeft) {
        // Activity bar width is 48px (w-12)
        const newWidth = Math.min(Math.max(e.clientX - 48, 140), 450);
        setSidebarWidth(newWidth);
      } else if (isDraggingRight) {
        const newWidth = Math.min(Math.max(window.innerWidth - e.clientX, 200), 550);
        setRightPanelWidth(newWidth);
      }
    };

    const handleMouseUp = () => {
      setIsDraggingLeft(false);
      setIsDraggingRight(false);
    };

    if (isDraggingLeft || isDraggingRight) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    } else {
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isDraggingLeft, isDraggingRight]);

  // Browser Address Bar
  const [browserUrl, setBrowserUrl] = useState<string>('https://www.tradingview.com/chart/?symbol=EURUSD');

  // AI Vision Analysis State
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisStep, setAnalysisStep] = useState<string>('');
  const [copied, setCopied] = useState(false);

  // Risk Engine State
  const [balance, setBalance] = useState<number>(10000);
  const [riskPercent, setRiskPercent] = useState<number>(1.0);
  const [entryPrice, setEntryPrice] = useState<number>(1.08320);
  const [slPrice, setSlPrice] = useState<number>(1.08150);
  const [tpPrice, setTpPrice] = useState<number>(1.08660);

  // AI Chat Assistant State
  const [chatInput, setChatInput] = useState('');
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: '1',
      sender: 'gemini',
      text: 'Salam Trader! Saya **TradePilot Gemini AI Copilot**. Saya dapat menganalisis grafik TradingView secara real-time, mendeteksi Smart Money Concepts (OB, FVG, Liquidity Sweeps), dan menghitung posisi lot optimal.',
      timestamp: '18:50:00'
    }
  ]);

  // AI Signal Output Data
  const [aiResult, setAiResult] = useState({
    pair: 'EUR/USD',
    timeframe: 'H1',
    trend: 'Bullish Continuation',
    signal: 'BUY',
    confidence: 91,
    entry: '1.08320 - 1.08350',
    stopLoss: '1.08150 (-17 pips)',
    takeProfit: '1.08660 (+34 pips)',
    riskReward: '1 : 2.0',
    reasoning: 'Harga merespons Asian Low (SSL) Sweep + M15 Order Block belum ter-mitigasi dengan FVG valid.',
    methods: ['ICT Liquidity Sweep', 'SMC Order Block (OB)', 'Fair Value Gap (FVG)', 'CHOCH M15', 'Discount Zone'],
    timestamp: '18:53:14'
  });

  // Sync Browser URL when pair changes
  useEffect(() => {
    const cleanSymbol = selectedPair.replace('/', '');
    setBrowserUrl(`https://www.tradingview.com/chart/?symbol=${cleanSymbol}&tf=${selectedTimeframe}`);
  }, [selectedPair, selectedTimeframe]);

  // Handle AI Vision Trigger
  const triggerAIAnalysis = () => {
    setIsAnalyzing(true);
    setAnalysisStep('> Capturing TradingView chart viewport frame...');

    setTimeout(() => {
      setAnalysisStep('> Dispatching image payload to Cloudflare Worker D1 Gateway...');
    }, 700);

    setTimeout(() => {
      setAnalysisStep('> Gemini 2.5 Flash evaluating SMC Order Blocks, FVG Imbalances & SSL Sweeps...');
    }, 1500);

    setTimeout(() => {
      setIsAnalyzing(false);
      const isXau = selectedPair === 'XAU/USD';
      const isBtc = selectedPair === 'BTC/USD';
      const isGbp = selectedPair === 'GBP/JPY';

      const newSignal = isXau ? 'SELL' : isBtc ? 'BUY' : isGbp ? 'SELL' : 'BUY';
      const conf = Math.floor(Math.random() * 8) + 88;

      let entry = '1.08320';
      let sl = '1.08150 (-17 pips)';
      let tp = '1.08660 (+34 pips)';

      if (isXau) {
        entry = '2,415.50';
        sl = '2,422.00 (-65 pips)';
        tp = '2,402.00 (+135 pips)';
      } else if (isBtc) {
        entry = '65,200.00';
        sl = '64,500.00 (-700 pips)';
        tp = '66,800.00 (+1,600 pips)';
      } else if (isGbp) {
        entry = '202.10';
        sl = '202.60 (-50 pips)';
        tp = '201.00 (+110 pips)';
      }

      setAiResult({
        pair: selectedPair,
        timeframe: selectedTimeframe,
        trend: isXau ? 'Bearish Rejection' : isBtc ? 'Bullish Breakout' : 'Range Contraction',
        signal: newSignal,
        confidence: conf,
        entry,
        stopLoss: sl,
        takeProfit: tp,
        riskReward: '1 : 2.1',
        reasoning: `Struktur ${selectedPair} (${selectedTimeframe}) mengkonfirmasi bias ${newSignal}. Rejection dari Key Institutional Level terdeteksi.`,
        methods: ['ICT Liquidity Sweep', 'SMC Order Block (OB)', 'Fair Value Gap (FVG)', 'PDH/PDL Sweep'],
        timestamp: new Date().toLocaleTimeString()
      });
      setRightPanelTab('ai');
    }, 2200);
  };

  // Handle Send Chat
  const handleSendChat = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!chatInput.trim()) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: chatInput,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setChatMessages((prev) => [...prev, userMsg]);
    const query = chatInput;
    setChatInput('');

    setTimeout(() => {
      let replyText = `Berdasarkan analisis chart ${selectedPair} (${selectedTimeframe}), struktur pasar menunjukkan konfirmasi setup **${aiResult.signal}** dengan rasio Risk/Reward **${aiResult.riskReward}**.`;
      let codeSnippet = undefined;

      if (query.toLowerCase().includes('lot') || query.toLowerCase().includes('risk')) {
        replyText = `Kalkulasi Lot untuk akun **$${balance.toLocaleString()}** dengan risiko **${riskPercent}%** ($${(balance * riskPercent / 100).toFixed(2)}):`;
        codeSnippet = `// TradePilot Smart Risk Engine
const lotSize = ${(balance * (riskPercent / 100) / (17 * 10)).toFixed(2)}; // Recommended Lots
const maxLossUSD = $${(balance * (riskPercent / 100)).toFixed(2)};
const riskReward = "${aiResult.riskReward}";`;
      }

      const botMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'gemini',
        text: replyText,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        codeSnippet
      };
      setChatMessages((prev) => [...prev, botMsg]);
    }, 800);
  };

  // Risk Lot Calculations
  const riskAmount = balance * (riskPercent / 100);
  const pipsSL = Math.abs(entryPrice - slPrice) * (selectedPair === 'XAU/USD' ? 10 : selectedPair === 'BTC/USD' ? 0.1 : 10000);
  const calculatedLot = pipsSL > 0 ? (riskAmount / (pipsSL * 10)).toFixed(2) : '0.00';

  const copySignalJson = () => {
    navigator.clipboard.writeText(JSON.stringify(aiResult, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="h-screen w-screen bg-[#1e1e1e] text-[#cccccc] font-sans flex flex-col overflow-hidden select-none">
      
      {/* ========================================================= */}
      {/* 1. VS CODE TOP TITLEBAR */}
      {/* ========================================================= */}
      <div className="h-9 bg-[#323233] border-b border-[#252526] px-3 flex items-center justify-between text-xs shrink-0 font-mono">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-[#ff5f56] inline-block border border-[#e0443e]"></span>
            <span className="w-3 h-3 rounded-full bg-[#ffbd2e] inline-block border border-[#dea123]"></span>
            <span className="w-3 h-3 rounded-full bg-[#27c93f] inline-block border border-[#1aab29]"></span>
          </div>
          <span className="text-gray-300 text-[11px] font-sans flex items-center gap-1.5">
            <Code2 className="w-3.5 h-3.5 text-[#007acc]" />
            <strong className="text-white">TradePilot AI Browser</strong>
            <span className="text-gray-500">— [VS Code Resizable Layout]</span>
          </span>
        </div>

        {/* Command Palette Bar */}
        <div className="hidden md:flex items-center bg-[#2a2a2b] border border-[#3c3c3c] rounded px-3 py-0.5 text-[11px] text-gray-400 w-96 justify-between cursor-pointer hover:border-[#007acc] transition-colors">
          <span className="flex items-center gap-2">
            <Terminal className="w-3 h-3 text-[#4ec9b0]" />
            <span>tradepilot-ai: run gemini vision analysis</span>
          </span>
          <span className="text-[10px] bg-[#3a3a3c] px-1.5 py-0.2 rounded text-gray-300">Ctrl+Shift+P</span>
        </div>

        {/* Status Badges */}
        <div className="flex items-center gap-3 text-[11px]">
          <span className="text-[#6a9955] flex items-center gap-1 font-bold">
            <span className="w-2 h-2 rounded-full bg-[#6a9955] animate-pulse"></span>
            TradingView Live API
          </span>
          <span className="text-[#ce9178] bg-[#2d2d2d] px-2 py-0.5 rounded border border-[#3c3c3c]">
            Gemini 2.5 Flash
          </span>
        </div>
      </div>

      {/* ========================================================= */}
      {/* 2. MAIN WORKSPACE */}
      {/* ========================================================= */}
      <div className="flex-1 flex overflow-hidden relative">

        {/* ------------------------------------------------------- */}
        {/* ACTIVITY BAR (FAR LEFT) - 5 ICONS */}
        {/* ------------------------------------------------------- */}
        <div className="w-12 bg-[#181818] border-r border-[#252526] flex flex-col justify-between items-center py-2 shrink-0 z-20">
          <div className="flex flex-col gap-1 w-full">
            {[
              { id: 'explorer', icon: FolderOpen, label: 'File Explorer' },
              { id: 'chart', icon: LineChart, label: 'Chart Browser' },
              { id: 'aichat', icon: Sparkles, label: '✨ AI Copilot Chat' },
              { id: 'risk', icon: Calculator, label: '🧮 Risk Calculator' },
              { id: 'settings', icon: Settings, label: 'Settings' },
            ].map((tab) => {
              const Icon = tab.icon;
              const isActive = activeActivityTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveActivityTab(tab.id as ActivityTab)}
                  title={tab.label}
                  className={`w-full h-11 flex items-center justify-center relative transition-colors ${
                    isActive ? 'text-white bg-[#252526]' : 'text-[#858585] hover:text-white'
                  }`}
                >
                  {isActive && <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-[#007acc]"></div>}
                  <Icon className="w-5 h-5" />
                </button>
              );
            })}
          </div>

          <div className="flex flex-col gap-2 text-[#858585]">
            <button
              onClick={triggerAIAnalysis}
              className="p-2 hover:text-white transition-colors cursor-pointer"
              title="Run AI Vision Analysis"
            >
              <Play className="w-4 h-4 text-[#6a9955]" />
            </button>
          </div>
        </div>

        {/* ------------------------------------------------------- */}
        {/* SIDEBAR (RESIZABLE) */}
        {/* ------------------------------------------------------- */}
        <div
          style={{ width: `${sidebarWidth}px` }}
          className="bg-[#252526] flex flex-col shrink-0 text-xs select-none overflow-hidden relative"
        >
          <div className="h-9 px-3 flex items-center justify-between text-[11px] font-bold uppercase tracking-wider text-[#bbbbbb] border-b border-[#1e1e1e] shrink-0">
            <span className="truncate">EXPLORER</span>
            <span className="text-[#007acc] shrink-0">WATCHLIST</span>
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-3 font-mono text-[11px]">
            {/* Pairs Folder */}
            <div>
              <div className="flex items-center gap-1.5 text-gray-300 font-bold py-1 px-1">
                <ChevronRight className="w-3.5 h-3.5 text-gray-400 rotate-90 transition-transform shrink-0" />
                <FolderOpen className="w-4 h-4 text-[#dcb67a] shrink-0" />
                <span className="truncate">tradingview-charts</span>
              </div>

              <div className="pl-4 space-y-0.5 mt-1">
                <div className="text-gray-500 uppercase font-sans text-[10px] font-bold pt-1 pb-1 tracking-wider">
                  // ACTIVE PAIRS
                </div>

                {[
                  { symbol: 'EUR/USD', file: 'EURUSD.chart.ts', color: 'text-[#4ec9b0]' },
                  { symbol: 'XAU/USD', file: 'XAUUSD.chart.ts', color: 'text-[#ce9178]' },
                  { symbol: 'BTC/USD', file: 'BTCUSD.chart.ts', color: 'text-[#dcdcaa]' },
                  { symbol: 'GBP/JPY', file: 'GBPJPY.chart.ts', color: 'text-[#c586c0]' },
                ].map((item) => {
                  const isSelected = selectedPair === item.symbol;
                  return (
                    <button
                      key={item.symbol}
                      onClick={() => {
                        setSelectedPair(item.symbol as PairSymbol);
                        setActiveActivityTab('chart');
                      }}
                      className={`w-full text-left py-1 px-2 rounded flex items-center justify-between transition-colors ${
                        isSelected ? 'bg-[#37373d] text-white font-bold' : 'text-gray-400 hover:bg-[#2a2d2e] hover:text-gray-200'
                      }`}
                    >
                      <span className="flex items-center gap-1.5 truncate">
                        <FileCode className={`w-3.5 h-3.5 ${item.color} shrink-0`} />
                        <span className="truncate">{item.symbol}</span>
                      </span>
                      {isSelected && <span className="w-1.5 h-1.5 rounded-full bg-[#007acc] shrink-0"></span>}
                    </button>
                  );
                })}

                <div className="text-gray-500 uppercase font-sans text-[10px] font-bold pt-3 pb-1 tracking-wider">
                  // TIMEFRAMES
                </div>

                <div className="grid grid-cols-3 gap-1 pt-1">
                  {(['M5', 'M15', 'H1', 'H4', 'D1'] as Timeframe[]).map((tf) => (
                    <button
                      key={tf}
                      onClick={() => setSelectedTimeframe(tf)}
                      className={`py-1 text-center rounded text-[10px] border transition-colors ${
                        selectedTimeframe === tf
                          ? 'bg-[#007acc] text-white border-[#007acc] font-bold'
                          : 'bg-[#1e1e1e] text-gray-400 border-[#3c3c3c] hover:text-white'
                      }`}
                    >
                      {tf}
                    </button>
                  ))}
                </div>

              </div>
            </div>

            {/* Overlays Controls */}
            <div className="p-2.5 bg-[#1e1e1e] border border-[#3c3c3c] rounded text-[10px] space-y-2 mt-4">
              <div className="flex justify-between items-center">
                <span className="text-[#4ec9b0] font-bold flex items-center gap-1 truncate">
                  <Layers className="w-3 h-3 shrink-0" /> SMC Overlays
                </span>
                <button
                  onClick={() => setShowOverlays(!showOverlays)}
                  className={`px-2 py-0.5 rounded font-bold text-[9px] shrink-0 cursor-pointer ${
                    showOverlays ? 'bg-[#007acc] text-white' : 'bg-[#2d2d2d] text-gray-400'
                  }`}
                >
                  {showOverlays ? 'ON' : 'OFF'}
                </button>
              </div>
              <p className="text-gray-400 font-sans leading-tight">
                Renders Order Blocks, FVG Imbalances & Liquidity Sweeps on top of TradingView.
              </p>
            </div>

          </div>
        </div>

        {/* ------------------------------------------------------- */}
        {/* RESIZABLE DRAG HANDLE (LEFT SIDEBAR <-> CENTER BROWSER) */}
        {/* ------------------------------------------------------- */}
        <div
          onMouseDown={() => setIsDraggingLeft(true)}
          className={`w-1 hover:w-1.5 bg-[#1e1e1e] hover:bg-[#007acc] active:bg-[#007acc] cursor-col-resize transition-all shrink-0 z-30 group relative flex items-center justify-center ${
            isDraggingLeft ? 'bg-[#007acc] w-1.5' : ''
          }`}
          title="Geser untuk mengubah lebar sidebar"
        >
          <div className="w-0.5 h-6 bg-gray-600 group-hover:bg-white rounded-full transition-colors opacity-0 group-hover:opacity-100"></div>
        </div>

        {/* ------------------------------------------------------- */}
        {/* CENTER MAIN CONTENT: DEDICATED TRADING BROWSER */}
        {/* ------------------------------------------------------- */}
        <div className="flex-1 bg-[#1e1e1e] flex flex-col min-w-0 overflow-hidden">
          
          {/* 1. BROWSER ADDRESS BAR & TAB HEADER */}
          <div className="bg-[#252526] border-b border-[#1e1e1e] flex flex-col shrink-0 select-none">
            
            {/* Top Editor Tabs */}
            <div className="h-8 flex items-center overflow-x-auto border-b border-[#1e1e1e] text-xs font-mono">
              <div className="h-full bg-[#1e1e1e] border-t-2 border-t-[#007acc] border-r border-r-[#252526] px-3.5 flex items-center gap-2 text-white font-bold shrink-0">
                <Globe className="w-3.5 h-3.5 text-[#4ec9b0]" />
                <span>TradingView ({selectedPair})</span>
                <span className="text-[10px] bg-[#007acc] px-1 py-0.2 rounded text-white font-mono">{selectedTimeframe}</span>
              </div>

              <div
                onClick={() => setActiveActivityTab('aichat')}
                className="h-full px-3.5 flex items-center gap-2 text-gray-400 border-r border-r-[#1e1e1e] hover:bg-[#2d2d2d] cursor-pointer shrink-0"
              >
                <Sparkles className="w-3.5 h-3.5 text-[#ce9178]" />
                <span>copilot_chat.ts</span>
              </div>

              <div
                onClick={() => setActiveActivityTab('risk')}
                className="h-full px-3.5 flex items-center gap-2 text-gray-400 border-r border-r-[#1e1e1e] hover:bg-[#2d2d2d] cursor-pointer shrink-0"
              >
                <Calculator className="w-3.5 h-3.5 text-[#dcdcaa]" />
                <span>risk_calculator.py</span>
              </div>
            </div>

            {/* Address Bar (URL Bar styled like VS Code Command Palette) */}
            <div className="p-1.5 px-3 flex items-center gap-2 bg-[#1e1e1e]">
              <div className="flex items-center gap-1 text-gray-400">
                <button className="p-1 hover:text-white hover:bg-[#2d2d2d] rounded cursor-pointer"><ChevronLeft className="w-3.5 h-3.5" /></button>
                <button className="p-1 hover:text-white hover:bg-[#2d2d2d] rounded cursor-pointer"><ChevronRight className="w-3.5 h-3.5" /></button>
                <button className="p-1 hover:text-white hover:bg-[#2d2d2d] rounded cursor-pointer" onClick={triggerAIAnalysis}><RefreshCw className="w-3.5 h-3.5" /></button>
              </div>

              {/* URL Input Box */}
              <div className="flex-1 flex items-center bg-[#252526] border border-[#3c3c3c] rounded px-3 py-1 text-xs text-gray-300 font-mono hover:border-[#007acc] focus-within:border-[#007acc]">
                <Lock className="w-3 h-3 text-[#6a9955] mr-2 shrink-0" />
                <span className="text-gray-500 mr-0.5">https://</span>
                <input
                  type="text"
                  value={browserUrl}
                  onChange={(e) => setBrowserUrl(e.target.value)}
                  className="bg-transparent text-white w-full focus:outline-none text-[11px]"
                />
                <Bookmark className="w-3.5 h-3.5 text-gray-500 hover:text-white cursor-pointer ml-2 shrink-0" />
              </div>

              {/* Overlay Toggle Pill */}
              <button
                onClick={() => setShowOverlays(!showOverlays)}
                className={`px-3 py-1 rounded text-[11px] font-mono font-bold flex items-center gap-1.5 transition-colors cursor-pointer shrink-0 ${
                  showOverlays ? 'bg-[#007acc] text-white' : 'bg-[#2d2d2d] text-gray-400 border border-[#3c3c3c]'
                }`}
              >
                {showOverlays ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                <span>SMC OVERLAYS</span>
              </button>
            </div>

          </div>

          {/* 2. DYNAMIC CONTENT VIEWS */}
          <div className="flex-1 overflow-y-auto p-4 relative bg-[#181818] flex flex-col justify-between">
            
            {/* VIEW 1: TRADINGVIEW CHART BROWSER */}
            {activeActivityTab === 'chart' && (
              <div className="h-full flex flex-col space-y-3">
                
                {/* Header Info Bar inside Browser */}
                <div className="flex flex-wrap items-center justify-between gap-3 bg-[#252526] border border-[#3c3c3c] p-2.5 rounded-md font-mono text-xs">
                  <div className="flex items-center gap-4">
                    <div>
                      <span className="text-gray-500 text-[10px] uppercase block">Symbol</span>
                      <span className="text-white font-bold text-sm">{selectedPair}</span>
                    </div>

                    <div className="h-6 w-px bg-[#3c3c3c]"></div>

                    <div>
                      <span className="text-gray-500 text-[10px] uppercase block">Timeframe</span>
                      <span className="text-[#007acc] font-bold">{selectedTimeframe}</span>
                    </div>

                    <div className="h-6 w-px bg-[#3c3c3c]"></div>

                    <div>
                      <span className="text-gray-500 text-[10px] uppercase block">Current Price</span>
                      <span className="text-[#6a9955] font-bold text-sm">
                        {selectedPair === 'XAU/USD' ? '2,415.80' : selectedPair === 'BTC/USD' ? '65,420.00' : '1.08425'}
                      </span>
                    </div>
                  </div>

                  {/* Run Gemini Vision Button */}
                  <button
                    onClick={triggerAIAnalysis}
                    disabled={isAnalyzing}
                    className="bg-[#007acc] hover:bg-[#0062a3] text-white font-bold px-4 py-1.5 rounded flex items-center gap-2 transition-all cursor-pointer shadow-[0_0_12px_rgba(0,122,204,0.3)] text-xs"
                  >
                    <Sparkles className={`w-3.5 h-3.5 ${isAnalyzing ? 'animate-spin' : ''}`} />
                    <span>{isAnalyzing ? 'ANALYZING...' : '✨ GEMINI AI ANALYSIS'}</span>
                  </button>
                </div>

                {/* TradingView Simulated Interactive Chart Canvas */}
                <div className="flex-1 min-h-[380px] bg-[#1e1e1e] border border-[#3c3c3c] rounded-md p-5 relative flex flex-col justify-between overflow-hidden">
                  
                  {/* Background Watermark */}
                  <div className="absolute right-6 top-6 text-right opacity-10 font-mono select-none pointer-events-none">
                    <div className="text-6xl font-black text-gray-200">{selectedPair}</div>
                    <div className="text-2xl font-bold text-gray-400">TRADINGVIEW BROWSER</div>
                  </div>

                  {/* DYNAMIC INTERNAL OVERLAYS (TradePilot SMC) */}
                  {showOverlays && (
                    <>
                      {/* Institutional Order Block (OB) Overlay */}
                      <div className="absolute top-10 left-16 w-56 h-22 bg-[#6a9955]/15 border border-[#6a9955] rounded p-2 text-xs font-mono text-[#6a9955] flex flex-col justify-between backdrop-blur-xs shadow-lg animate-pulse">
                        <div className="flex justify-between items-center text-[10px] font-bold">
                          <span className="flex items-center gap-1">
                            <Layers className="w-3 h-3" /> M15 Institutional OB
                          </span>
                          <span className="bg-[#6a9955] text-black px-1.5 py-0.2 rounded font-bold text-[9px]">
                            BUY ZONE
                          </span>
                        </div>
                        <div className="text-[10px] text-gray-300">
                          Range: {selectedPair === 'XAU/USD' ? '2410 - 2415' : '1.08320 - 1.08350'}
                        </div>
                      </div>

                      {/* Fair Value Gap (FVG) Overlay */}
                      <div className="absolute top-36 left-80 w-44 h-14 bg-[#ce9178]/15 border border-dashed border-[#ce9178] rounded p-2 text-[10px] font-mono text-[#ce9178]">
                        <span className="font-bold block text-[11px]">Fair Value Gap (FVG)</span>
                        <span className="text-[9px] text-gray-400">Unfilled Liquidity Imbalance</span>
                      </div>

                      {/* Liquidity Sweep (SSL) Line */}
                      <div className="absolute bottom-12 left-0 right-0 border-b-2 border-dashed border-[#f44747] flex justify-between text-[10px] text-[#f44747] font-mono px-4">
                        <span>Asian Low Sell-Side Liquidity (SSL)</span>
                        <span className="bg-[#f44747]/20 px-2 py-0.5 rounded border border-[#f44747]">
                          Swept ✓
                        </span>
                      </div>
                    </>
                  )}

                  {/* Interactive Candlesticks */}
                  <div className="my-auto h-60 w-full relative flex items-end justify-between px-6">
                    {[
                      { h: 35, isUp: true },
                      { h: 55, isUp: false },
                      { h: 40, isUp: true },
                      { h: 75, isUp: true },
                      { h: 50, isUp: false },
                      { h: 90, isUp: true },
                      { h: 65, isUp: true },
                      { h: 35, isUp: false },
                      { h: 100, isUp: true },
                      { h: 80, isUp: true },
                      { h: 45, isUp: false },
                      { h: 110, isUp: true },
                    ].map((candle, idx) => (
                      <div key={idx} className="flex flex-col items-center justify-end h-full group cursor-crosshair">
                        <div className="w-0.5 h-full bg-[#4a4a4a] group-hover:bg-white transition-colors"></div>
                        <div
                          className={`w-4 rounded-2xs transition-all ${
                            candle.isUp ? 'bg-[#6a9955] group-hover:bg-[#7bc264]' : 'bg-[#f44747] group-hover:bg-[#ff5f5f]'
                          }`}
                          style={{ height: `${candle.h}%` }}
                        ></div>
                      </div>
                    ))}
                  </div>

                  {/* Canvas Footer */}
                  <div className="flex justify-between items-center text-[11px] font-mono text-gray-500 border-t border-[#2d2d2d] pt-2">
                    <span className="flex items-center gap-2">
                      <Activity className="w-3.5 h-3.5 text-[#4ec9b0]" />
                      <span>SMC Indicator Engine v2.4</span>
                    </span>
                    <span className="text-[#6a9955]">WEBSOCKET: CONNECTED (24ms)</span>
                  </div>

                </div>

                {/* AI Progress Modal */}
                {isAnalyzing && (
                  <div className="absolute inset-0 bg-[#181818]/90 backdrop-blur-sm z-50 flex flex-col items-center justify-center p-6 font-mono text-xs">
                    <div className="w-12 h-12 border-4 border-[#007acc] border-t-transparent rounded-full animate-spin mb-4"></div>
                    <div className="text-[#4ec9b0] font-bold text-sm uppercase tracking-wider mb-2">
                      Cloudflare Worker D1 Gateway
                    </div>
                    <div className="bg-[#252526] border border-[#3c3c3c] text-gray-200 px-4 py-2 rounded text-[11px] max-w-md text-center shadow-2xl">
                      {analysisStep}
                    </div>
                  </div>
                )}

              </div>
            )}

            {/* VIEW 2: ✨ AI COPILOT CHAT */}
            {activeActivityTab === 'aichat' && (
              <div className="h-full flex flex-col font-mono text-xs justify-between space-y-3">
                <div className="border-b border-[#3c3c3c] pb-2 flex justify-between items-center">
                  <h2 className="text-sm font-bold text-[#4ec9b0] flex items-center gap-2">
                    <Sparkles className="w-4 h-4" /> GEMINI AI COPILOT CHAT
                  </h2>
                  <span className="text-[10px] text-gray-500">models/gemini-2.5-flash</span>
                </div>

                <div className="flex-1 bg-[#1e1e1e] border border-[#3c3c3c] rounded p-4 overflow-y-auto space-y-3">
                  {chatMessages.map((msg) => (
                    <div
                      key={msg.id}
                      className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
                    >
                      <div
                        className={`max-w-[85%] rounded p-3 text-xs leading-relaxed ${
                          msg.sender === 'user'
                            ? 'bg-[#007acc] text-white'
                            : 'bg-[#252526] border border-[#3c3c3c] text-gray-200'
                        }`}
                      >
                        <p>{msg.text}</p>
                        {msg.codeSnippet && (
                          <pre className="mt-2 p-2 bg-[#1e1e1e] border border-[#3c3c3c] rounded text-[10px] text-[#4ec9b0] overflow-x-auto">
                            {msg.codeSnippet}
                          </pre>
                        )}
                      </div>
                      <span className="text-[9px] text-gray-500 mt-1">{msg.timestamp}</span>
                    </div>
                  ))}
                </div>

                <form onSubmit={handleSendChat} className="flex gap-2">
                  <input
                    type="text"
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    placeholder="Tanyakan analisis SMC, hitung lot, atau bantuan strategi..."
                    className="flex-1 bg-[#252526] border border-[#3c3c3c] rounded px-3 py-2 text-white focus:border-[#007acc] focus:outline-none"
                  />
                  <button
                    type="submit"
                    className="bg-[#007acc] hover:bg-[#0062a3] text-white px-4 rounded font-bold flex items-center justify-center cursor-pointer"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </form>
              </div>
            )}

            {/* VIEW 3: 🧮 RISK CALCULATOR */}
            {activeActivityTab === 'risk' && (
              <div className="h-full space-y-4 font-mono text-xs">
                <div className="border-b border-[#3c3c3c] pb-2">
                  <h2 className="text-sm font-bold text-[#dcdcaa] flex items-center gap-2">
                    <Calculator className="w-4 h-4" /> SMART RISK & LOT ENGINE
                  </h2>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-[#252526] p-4 rounded border border-[#3c3c3c] space-y-3">
                    <div>
                      <label className="text-gray-400 text-[10px] block mb-1">Account Balance (USD)</label>
                      <input
                        type="number"
                        value={balance}
                        onChange={(e) => setBalance(Number(e.target.value))}
                        className="w-full bg-[#1e1e1e] border border-[#3c3c3c] rounded p-2 text-white font-bold"
                      />
                    </div>

                    <div>
                      <div className="flex justify-between text-[10px] text-gray-400 mb-1">
                        <span>Risk Percentage</span>
                        <span className="text-[#6a9955] font-bold">${riskAmount.toFixed(2)} USD</span>
                      </div>
                      <input
                        type="range"
                        min="0.5"
                        max="5.0"
                        step="0.5"
                        value={riskPercent}
                        onChange={(e) => setRiskPercent(Number(e.target.value))}
                        className="w-full accent-[#007acc]"
                      />
                      <div className="text-right text-[#007acc] font-bold">{riskPercent}%</div>
                    </div>
                  </div>

                  <div className="bg-[#1e1e1e] border border-[#007acc] p-4 rounded flex flex-col items-center justify-center text-center space-y-2">
                    <span className="text-[10px] text-[#4ec9b0] font-bold uppercase block">// Recommended Lot Size</span>
                    <div className="text-3xl font-black text-white">
                      {calculatedLot} <span className="text-sm text-[#6a9955]">LOTS</span>
                    </div>
                    <p className="text-[10px] text-gray-400 max-w-xs">
                      Max loss is capped at ${riskAmount.toFixed(2)} USD for {pipsSL.toFixed(1)} pips stop loss distance.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* VIEW 4: FILE EXPLORER / SETTINGS */}
            {activeActivityTab === 'settings' && (
              <div className="space-y-4 font-mono text-xs">
                <div className="border-b border-[#3c3c3c] pb-2">
                  <h2 className="text-sm font-bold text-[#ce9178]">TRADEPILOT GATEWAY CONFIGURATION</h2>
                </div>

                <div className="bg-[#252526] p-4 rounded border border-[#3c3c3c] space-y-3">
                  <div>
                    <label className="text-gray-400 text-[10px] uppercase block mb-1">Cloudflare Worker Gateway</label>
                    <input
                      type="text"
                      readOnly
                      value="https://tradepilot-ai-gateway.workers.dev/api/v1/analyze"
                      className="w-full bg-[#1e1e1e] border border-[#3c3c3c] rounded p-2 text-gray-200"
                    />
                  </div>

                  <div>
                    <label className="text-gray-400 text-[10px] uppercase block mb-1">AI Vision Provider</label>
                    <input
                      type="text"
                      readOnly
                      value="models/gemini-2.5-flash (Google GenAI)"
                      className="w-full bg-[#1e1e1e] border border-[#3c3c3c] rounded p-2 text-[#4ec9b0]"
                    />
                  </div>
                </div>
              </div>
            )}

          </div>

          {/* Bottom VS Code Status Bar */}
          <div className="h-6 bg-[#007acc] text-white flex items-center justify-between px-3 text-[11px] font-mono shrink-0 select-none">
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1 font-bold">
                <GitBranch className="w-3 h-3" /> main*
              </span>
              <span>0 errors, 0 warnings</span>
              <span className="hidden sm:inline">| UTF-8</span>
            </div>

            <div className="flex items-center gap-3 text-[10px]">
              <span>TradingView Live</span>
              <span>Cloudflare D1: OK</span>
              <span>Gemini 2.5 Flash</span>
            </div>
          </div>

        </div>

        {/* ------------------------------------------------------- */}
        {/* RESIZABLE DRAG HANDLE (CENTER BROWSER <-> RIGHT PANEL) */}
        {/* ------------------------------------------------------- */}
        <div
          onMouseDown={() => setIsDraggingRight(true)}
          className={`w-1 hover:w-1.5 bg-[#1e1e1e] hover:bg-[#007acc] active:bg-[#007acc] cursor-col-resize transition-all shrink-0 z-30 group relative flex items-center justify-center ${
            isDraggingRight ? 'bg-[#007acc] w-1.5' : ''
          }`}
          title="Geser untuk mengubah lebar panel kanan"
        >
          <div className="w-0.5 h-6 bg-gray-600 group-hover:bg-white rounded-full transition-colors opacity-0 group-hover:opacity-100"></div>
        </div>

        {/* ------------------------------------------------------- */}
        {/* RIGHT PANEL (COPILOT AI ANALYSIS & TERMINAL OUTPUT) */}
        {/* ------------------------------------------------------- */}
        <aside
          style={{ width: `${rightPanelWidth}px` }}
          className="bg-[#252526] flex flex-col shrink-0 overflow-hidden text-xs font-mono"
        >
          
          {/* Panel Header Tabs */}
          <div className="h-9 bg-[#2d2d2d] flex items-center border-b border-[#1e1e1e] text-[11px] font-mono shrink-0 select-none">
            <button
              onClick={() => setRightPanelTab('ai')}
              className={`h-full px-3 flex items-center gap-1.5 border-b-2 font-bold cursor-pointer ${
                rightPanelTab === 'ai'
                  ? 'border-[#007acc] text-white bg-[#252526]'
                  : 'border-transparent text-gray-400 hover:text-white'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5 text-[#4ec9b0]" />
              <span>✨ GEMINI AI</span>
            </button>

            <button
              onClick={() => setRightPanelTab('risk')}
              className={`h-full px-3 flex items-center gap-1.5 border-b-2 font-bold cursor-pointer ${
                rightPanelTab === 'risk'
                  ? 'border-[#007acc] text-white bg-[#252526]'
                  : 'border-transparent text-gray-400 hover:text-white'
              }`}
            >
              <ShieldCheck className="w-3.5 h-3.5 text-[#dcdcaa]" />
              <span>🧮 RISK ENGINE</span>
            </button>
          </div>

          {/* Panel Body */}
          <div className="flex-1 overflow-y-auto p-3 space-y-4 font-mono text-xs">
            
            {/* 1. GEMINI AI ANALYSIS */}
            {rightPanelTab === 'ai' && (
              <div className="space-y-3">
                
                <div className="flex justify-between items-center border-b border-[#3c3c3c] pb-2">
                  <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">
                    // ANALYSIS OUTPUT
                  </span>
                  <button
                    onClick={copySignalJson}
                    className="text-[10px] text-gray-400 hover:text-white bg-[#1e1e1e] border border-[#3c3c3c] px-2 py-0.5 rounded flex items-center gap-1 cursor-pointer"
                  >
                    {copied ? <Check className="w-3 h-3 text-[#6a9955]" /> : <Copy className="w-3 h-3" />}
                    <span>{copied ? 'COPIED' : 'COPY JSON'}</span>
                  </button>
                </div>

                {/* Signal Badge */}
                <div className="p-3 bg-[#1e1e1e] border border-[#3c3c3c] rounded space-y-2">
                  <div className="text-[10px] text-gray-500">// Recommendation Signal</div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-400">const signal =</span>
                    <span className={`text-base font-black px-2.5 py-0.5 rounded ${
                      aiResult.signal === 'BUY'
                        ? 'bg-[#6a9955]/20 text-[#6a9955] border border-[#6a9955]/50'
                        : 'bg-[#f44747]/20 text-[#f44747] border border-[#f44747]/50'
                    }`}>
                      "{aiResult.signal}"
                    </span>
                  </div>
                  <div className="flex justify-between text-[11px]">
                    <span className="text-gray-400">const confidence =</span>
                    <span className="text-[#dcdcaa] font-bold">{aiResult.confidence}%</span>
                  </div>
                </div>

                {/* Code Object Setup */}
                <div className="p-3 bg-[#1e1e1e] border border-[#3c3c3c] rounded space-y-1.5 text-[11px]">
                  <div className="text-[#c586c0] font-bold">const tradeSetup = &#123;</div>
                  
                  <div className="pl-3 flex justify-between">
                    <span className="text-[#9cdcfe]">entryZone:</span>
                    <span className="text-[#ce9178]">"{aiResult.entry}"</span>
                  </div>

                  <div className="pl-3 flex justify-between">
                    <span className="text-[#9cdcfe]">stopLoss:</span>
                    <span className="text-[#f44747]">"{aiResult.stopLoss}"</span>
                  </div>

                  <div className="pl-3 flex justify-between">
                    <span className="text-[#9cdcfe]">takeProfit:</span>
                    <span className="text-[#6a9955]">"{aiResult.takeProfit}"</span>
                  </div>

                  <div className="pl-3 flex justify-between">
                    <span className="text-[#9cdcfe]">riskReward:</span>
                    <span className="text-[#dcdcaa]">"{aiResult.riskReward}"</span>
                  </div>

                  <div className="text-[#c586c0] font-bold">&#125;;</div>
                </div>

                {/* Technical Confirmations */}
                <div className="space-y-1">
                  <span className="text-[10px] text-gray-500 font-bold uppercase block">// Technical Confirmations</span>
                  <div className="space-y-1">
                    {aiResult.methods.map((m, idx) => (
                      <div key={idx} className="bg-[#1e1e1e] border border-[#3c3c3c] p-1.5 rounded text-[10px] text-[#4ec9b0] flex items-center gap-1.5">
                        <CheckCircle2 className="w-3 h-3 text-[#6a9955]" />
                        <span>{m}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* AI Model Reasoning */}
                <div className="p-2.5 bg-[#1e1e1e] border border-[#3c3c3c] rounded text-[11px] space-y-1 text-gray-300">
                  <span className="text-[#6a9955] block font-bold">/* AI Model Reasoning */</span>
                  <p className="font-sans text-gray-300 leading-snug">
                    {aiResult.reasoning}
                  </p>
                </div>

              </div>
            )}

            {/* 2. SMART RISK ENGINE */}
            {rightPanelTab === 'risk' && (
              <div className="space-y-3">
                <div className="border-b border-[#3c3c3c] pb-2">
                  <span className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">
                    // RISK & LOT ENGINE
                  </span>
                </div>

                <div className="space-y-2.5 text-xs">
                  <div>
                    <label className="text-[10px] text-gray-400 block mb-1">let balance =</label>
                    <input
                      type="number"
                      value={balance}
                      onChange={(e) => setBalance(Number(e.target.value))}
                      className="w-full bg-[#1e1e1e] border border-[#3c3c3c] rounded p-1.5 text-white font-mono focus:border-[#007acc] focus:outline-none"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-[10px] text-gray-400 mb-1">
                      <span>let riskPercent =</span>
                      <span className="text-[#6a9955]">${riskAmount.toFixed(2)} USD</span>
                    </div>
                    <input
                      type="range"
                      min="0.5"
                      max="5.0"
                      step="0.5"
                      value={riskPercent}
                      onChange={(e) => setRiskPercent(Number(e.target.value))}
                      className="w-full accent-[#007acc] cursor-pointer"
                    />
                    <div className="text-right text-[#007acc] font-bold">{riskPercent}%</div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-[10px] text-gray-400 block mb-1">entryPrice</label>
                      <input
                        type="number"
                        step="0.0001"
                        value={entryPrice}
                        onChange={(e) => setEntryPrice(Number(e.target.value))}
                        className="w-full bg-[#1e1e1e] border border-[#3c3c3c] rounded p-1.5 text-white"
                      />
                    </div>
                    <div>
                      <label className="text-[10px] text-gray-400 block mb-1">stopLossPrice</label>
                      <input
                        type="number"
                        step="0.0001"
                        value={slPrice}
                        onChange={(e) => setSlPrice(Number(e.target.value))}
                        className="w-full bg-[#1e1e1e] border border-[#3c3c3c] rounded p-1.5 text-white"
                      />
                    </div>
                  </div>

                  <div className="p-3 bg-[#1e1e1e] border border-[#007acc] rounded text-center space-y-1">
                    <span className="text-[10px] text-[#4ec9b0] font-bold uppercase block">// Recommended Lot</span>
                    <div className="text-2xl font-black text-white">
                      {calculatedLot} <span className="text-xs text-[#6a9955]">LOTS</span>
                    </div>
                    <div className="text-[10px] text-gray-400">
                      SL Distance: {pipsSL.toFixed(1)} pips
                    </div>
                  </div>
                </div>
              </div>
            )}

          </div>

        </aside>

      </div>

    </div>
  );
}
